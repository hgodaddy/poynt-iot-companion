package co.poynt.iot.companion.shared.iot.phase2;

import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import co.poynt.iot.companion.shared.device.DeviceInspector;
import co.poynt.iot.companion.shared.logging.EvidenceLogger;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Production Discover sequence:
 * 411 {@code /discovery/services} → mothership URL → {@code mothership/discover}.
 */
public final class DiscoverClient {

    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build();
    private final Gson gson = new Gson();
    private final EvidenceLogger logger;
    private final AtomicBoolean inFlight = new AtomicBoolean(false);

    public DiscoverClient(@NonNull EvidenceLogger logger) {
        this.logger = logger;
    }

    @NonNull
    public DiscoverResult discover(@NonNull String environment, @NonNull String serial, @Nullable String deviceId) {
        if (!inFlight.compareAndSet(false, true)) {
            logger.info("Discover already in flight, skipping");
            return DiscoverResult.inFlight();
        }
        try {
            return discoverInternal(environment, serial, deviceId);
        } finally {
            inFlight.set(false);
        }
    }

    @NonNull
    private DiscoverResult discoverInternal(@NonNull String environment, @NonNull String serial, @Nullable String deviceId) {
        String fourOneOne = fourOneOneUrl(environment);
        logger.info("411 Discover started url=" + fourOneOne);
        String mothership = fetchMothershipUrl(fourOneOne);
        if (TextUtils.isEmpty(mothership)) {
            mothership = DeviceInspector.systemProperty("persist.poynt.srvc.url.mothership", "");
        }
        if (TextUtils.isEmpty(mothership)) {
            return DiscoverResult.fail("411 did not return mothershipService.endPoint.address");
        }
        if (!mothership.endsWith("/")) {
            mothership = mothership + "/";
        }
        logger.pass("411 mothership=" + mothership);

        String uuid = TextUtils.isEmpty(deviceId) ? serial : deviceId;
        String osBuild = Build.VERSION.INCREMENTAL != null ? Build.VERSION.INCREMENTAL : "";
        String url = mothership + "discover";
        HttpUrl parsed = HttpUrl.parse(url);
        if (parsed == null) {
            return DiscoverResult.fail("Invalid Discover URL: " + url, mothership, null);
        }
        HttpUrl.Builder builder = parsed.newBuilder()
                .addQueryParameter("device_uuid", uuid)
                .addQueryParameter("serial_number", serial)
                .addQueryParameter("os_build_version", osBuild);
        Request request = new Request.Builder()
                .url(builder.build())
                .header("Poynt-Request-Id", UUID.randomUUID().toString())
                .get()
                .build();
        logger.info("IoT Discover started " + builder.build());
        try (Response response = http.newCall(request).execute()) {
            int code = response.code();
            ResponseBody body = response.body();
            String json = body != null ? body.string() : "";
            logger.info("Discover response HTTP " + code + " bytes=" + json.length());
            if (!response.isSuccessful()) {
                return DiscoverResult.fail("Discover HTTP " + code, mothership, null);
            }
            if (json.trim().isEmpty()) {
                logger.info("IoT disabled by server (empty Discover body)");
                return DiscoverResult.disabled(mothership);
            }
            DiscoveredIoTServices services = gson.fromJson(json, DiscoveredIoTServices.class);
            if (services == null || !services.hasIotConfig()) {
                logger.info("IoT disabled by server (no iot object)");
                return DiscoverResult.disabled(mothership);
            }
            if (!ProductionIotConstants.isValidEndpoint(services.getIotEndpoint())) {
                return DiscoverResult.fail("Invalid IoT endpoint from Discover: " + services.getIotEndpoint(),
                        mothership, services);
            }
            logger.pass("Discover IoT enabled endpoint=" + services.getIotEndpoint()
                    + " authorizer=" + services.getAuthorizerName());
            return DiscoverResult.ok(mothership, services);
        } catch (IOException e) {
            return DiscoverResult.fail("Discover network error: " + e.getMessage(), mothership, null);
        }
    }

    @Nullable
    private String fetchMothershipUrl(@NonNull String fourOneOne) {
        Request request = new Request.Builder()
                .url(fourOneOne)
                .header("Poynt-Request-Id", UUID.randomUUID().toString())
                .header("User-Agent", "PoyntIoTCompanion/0.2")
                .get()
                .build();
        try (Response response = http.newCall(request).execute()) {
            logger.info("411 HTTP " + response.code());
            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }
            String json = response.body().string();
            return extractMothership(json);
        } catch (Exception e) {
            logger.info("411 failed: " + e.getMessage());
            return null;
        }
    }

    @Nullable
    static String extractMothership(@NonNull String json) {
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) {
                return null;
            }
            JsonObject object = root.getAsJsonObject();
            String direct = addressOf(object.get("mothershipService"));
            if (direct != null) {
                return direct;
            }
            direct = addressOf(object.get("mothership"));
            if (direct != null) {
                return direct;
            }
            if (object.has("services") && object.get("services").isJsonObject()) {
                return addressOf(object.getAsJsonObject("services").get("mothershipService"));
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static String addressOf(@Nullable JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject service = element.getAsJsonObject();
        JsonElement endpoint = service.has("endPoint") ? service.get("endPoint") : service.get("endpoint");
        if (endpoint != null && endpoint.isJsonObject()) {
            JsonObject ep = endpoint.getAsJsonObject();
            if (ep.has("address")) {
                return ep.get("address").getAsString();
            }
            if (ep.has("url")) {
                return ep.get("url").getAsString();
            }
        }
        if (service.has("address")) {
            return service.get("address").getAsString();
        }
        return null;
    }

    @NonNull
    private static String fourOneOneUrl(@NonNull String environment) {
        String fromDevice = DeviceInspector.systemProperty("persist.poynt.pcm.discovery", "");
        if (!TextUtils.isEmpty(fromDevice)) {
            if (fromDevice.contains("/discovery")) {
                return fromDevice;
            }
            return fromDevice.endsWith("/") ? fromDevice + "discovery/services" : fromDevice + "/discovery/services";
        }
        if ("DEV".equalsIgnoreCase(environment) || "CI".equalsIgnoreCase(environment)) {
            return ProductionIotConstants.FOURONEONE_CI;
        }
        return ProductionIotConstants.FOURONEONE_LIVE;
    }

    public static final class DiscoverResult {
        public final boolean ok;
        public final boolean iotEnabled;
        public final String status;
        public final String detail;
        public final String mothershipUrl;
        public final DiscoveredIoTServices services;

        private DiscoverResult(boolean ok, boolean iotEnabled, String status, String detail,
                               String mothershipUrl, DiscoveredIoTServices services) {
            this.ok = ok;
            this.iotEnabled = iotEnabled;
            this.status = status;
            this.detail = detail;
            this.mothershipUrl = mothershipUrl;
            this.services = services;
        }

        static DiscoverResult ok(String mothership, DiscoveredIoTServices services) {
            return new DiscoverResult(true, true, "PASS", services.toString(), mothership, services);
        }

        static DiscoverResult disabled(String mothership) {
            return new DiscoverResult(false, false, "FAIL", "IoT disabled by Discover", mothership, null);
        }

        static DiscoverResult inFlight() {
            return new DiscoverResult(false, false, "FAIL", "Discover already in flight", "—", null);
        }

        static DiscoverResult fail(String detail) {
            return new DiscoverResult(false, false, "FAIL", detail, "—", null);
        }

        static DiscoverResult fail(String detail, String mothership, DiscoveredIoTServices services) {
            return new DiscoverResult(false, false, "FAIL", detail, mothership, services);
        }
    }
}
