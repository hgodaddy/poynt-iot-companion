package co.poynt.iot.companion.shared.diagnostics;

import android.util.Base64;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import co.poynt.iot.companion.shared.config.EnvironmentConfig;
import co.poynt.iot.companion.shared.iot.phase2.CompanionMqttClient;
import co.poynt.iot.companion.shared.iot.phase2.DiscoverClient;
import co.poynt.iot.companion.shared.iot.phase2.GdTokenStore;
import co.poynt.iot.companion.shared.iot.phase2.IotRuntimeState;
import co.poynt.iot.companion.shared.iot.phase2.JwtInspector;
import co.poynt.iot.companion.shared.iot.phase2.ProductionIotConstants;
import co.poynt.iot.companion.shared.logging.EvidenceLogger;

/**
 * Phase 4 negative paths. PASS means the companion correctly detected the failure.
 */
public final class NegativeTestHarness {

    private final EvidenceLogger logger;
    private final GdTokenStore tokenStore;
    private final CompanionMqttClient mqtt;
    private final DiscoverClient discoverClient;
    private final NetworkInspector network;
    private final EnvironmentConfig env;
    private final IotRuntimeState state;

    public NegativeTestHarness(
            @NonNull EvidenceLogger logger,
            @NonNull GdTokenStore tokenStore,
            @NonNull CompanionMqttClient mqtt,
            @NonNull DiscoverClient discoverClient,
            @NonNull NetworkInspector network,
            @NonNull EnvironmentConfig env,
            @NonNull IotRuntimeState state) {
        this.logger = logger;
        this.tokenStore = tokenStore;
        this.mqtt = mqtt;
        this.discoverClient = discoverClient;
        this.network = network;
        this.env = env;
        this.state = state;
    }

    @NonNull
    public List<NegativeScenarioResult> runToken() {
        List<NegativeScenarioResult> results = new ArrayList<>();
        results.add(tokenMissing());
        results.add(tokenExpired());
        results.add(tokenInvalid());
        return results;
    }

    @NonNull
    public List<NegativeScenarioResult> runDiscover() {
        List<NegativeScenarioResult> results = new ArrayList<>();
        results.add(discoverTimeout());
        results.add(discoverUnavailable());
        results.add(discoverDuplicate());
        results.add(discoverInvalidHost());
        return results;
    }

    @NonNull
    public List<NegativeScenarioResult> runMqtt() {
        List<NegativeScenarioResult> results = new ArrayList<>();
        results.add(mqttPublishDisconnected());
        results.add(mqttSubscribeDisconnected());
        results.add(mqttInvalidCredentials());
        results.add(mqttConnectTimeout());
        return results;
    }

    @NonNull
    public List<NegativeScenarioResult> runNetwork() {
        List<NegativeScenarioResult> results = new ArrayList<>();
        results.add(noInternetProbe());
        results.add(wifiInterruptAndRecover());
        return results;
    }

    @NonNull
    public List<NegativeScenarioResult> runAll() {
        List<NegativeScenarioResult> results = new ArrayList<>();
        results.addAll(runToken());
        results.addAll(runDiscover());
        results.addAll(runMqtt());
        results.addAll(runNetwork());
        return results;
    }

    @NonNull
    private NegativeScenarioResult tokenMissing() {
        String saved = tokenStore.current();
        try {
            tokenStore.clear();
            String obtained = tokenStore.obtain(false);
            if (obtained == null) {
                logger.error(ErrorCode.TOKEN_MISSING.code, "correctly detected missing token");
                return NegativeScenarioResult.pass("TOKEN_MISSING", ErrorCode.TOKEN_MISSING, "store empty as expected");
            }
            return NegativeScenarioResult.fail("TOKEN_MISSING", ErrorCode.TOKEN_MISSING, "token still present after clear");
        } finally {
            if (saved != null) {
                tokenStore.save(saved);
            }
        }
    }

    @NonNull
    private NegativeScenarioResult tokenExpired() {
        String saved = tokenStore.current();
        try {
            tokenStore.save(expiredJwt());
            JwtInspector inspect = JwtInspector.inspect(tokenStore.current());
            if (inspect.present && inspect.expired) {
                logger.error(ErrorCode.TOKEN_EXPIRED.code, inspect.summary);
                return NegativeScenarioResult.pass("TOKEN_EXPIRED", ErrorCode.TOKEN_EXPIRED, inspect.summary);
            }
            return NegativeScenarioResult.fail("TOKEN_EXPIRED", ErrorCode.TOKEN_EXPIRED, inspect.summary);
        } finally {
            restore(saved);
        }
    }

    @NonNull
    private NegativeScenarioResult tokenInvalid() {
        String saved = tokenStore.current();
        try {
            tokenStore.save("not-a-jwt");
            JwtInspector inspect = JwtInspector.inspect(tokenStore.current());
            boolean invalid = !inspect.present || inspect.expired || inspect.summary.contains("parse-error")
                    || inspect.summary.contains("not a JWT");
            if (invalid) {
                logger.error(ErrorCode.TOKEN_INVALID.code, inspect.summary);
                return NegativeScenarioResult.pass("TOKEN_INVALID", ErrorCode.TOKEN_INVALID, inspect.summary);
            }
            return NegativeScenarioResult.fail("TOKEN_INVALID", ErrorCode.TOKEN_INVALID, inspect.summary);
        } finally {
            restore(saved);
        }
    }

    @NonNull
    private NegativeScenarioResult discoverTimeout() {
        DiscoverClient.DiscoverResult result = discoverClient.probeUrl("https://192.0.2.1/", 2);
        boolean timedOut = !result.ok;
        if (timedOut) {
            logger.error(ErrorCode.DISCOVER_TIMEOUT.code, result.detail);
            return NegativeScenarioResult.pass("DISCOVER_TIMEOUT", ErrorCode.DISCOVER_TIMEOUT, result.detail);
        }
        return NegativeScenarioResult.fail("DISCOVER_TIMEOUT", ErrorCode.DISCOVER_TIMEOUT, result.detail);
    }

    @NonNull
    private NegativeScenarioResult discoverUnavailable() {
        DiscoverClient.DiscoverResult result = discoverClient.probeUrl(
                "https://fouroneone.poynt.net/discovery/services-does-not-exist", 8);
        boolean failed = !result.ok || (result.detail != null && result.detail.contains("HTTP"));
        if (failed) {
            logger.error(ErrorCode.DISCOVER_HTTP.code, result.detail);
            return NegativeScenarioResult.pass("DISCOVER_UNAVAILABLE", ErrorCode.DISCOVER_HTTP, result.detail);
        }
        return NegativeScenarioResult.fail("DISCOVER_UNAVAILABLE", ErrorCode.DISCOVER_HTTP, result.detail);
    }

    @NonNull
    private NegativeScenarioResult discoverDuplicate() {
        discoverClient.markInFlightForTest(true);
        try {
            DiscoverClient.DiscoverResult result = discoverClient.discover(env.environment(), "NEG-SERIAL", "neg-device");
            if (!result.ok && result.detail != null && result.detail.contains("in flight")) {
                logger.error(ErrorCode.DISCOVER_DUPLICATE.code, result.detail);
                return NegativeScenarioResult.pass("DISCOVER_DUPLICATE", ErrorCode.DISCOVER_DUPLICATE, result.detail);
            }
            return NegativeScenarioResult.fail("DISCOVER_DUPLICATE", ErrorCode.DISCOVER_DUPLICATE, result.detail);
        } finally {
            discoverClient.markInFlightForTest(false);
        }
    }

    @NonNull
    private NegativeScenarioResult discoverInvalidHost() {
        DiscoverClient.DiscoverResult result = discoverClient.probeUrl("https://not-a-real-poynt-host.invalid/", 3);
        if (!result.ok) {
            logger.error(ErrorCode.DISCOVER_INVALID.code, result.detail);
            return NegativeScenarioResult.pass("DISCOVER_INVALID_HOST", ErrorCode.DISCOVER_INVALID, result.detail);
        }
        return NegativeScenarioResult.fail("DISCOVER_INVALID_HOST", ErrorCode.DISCOVER_INVALID, result.detail);
    }

    @NonNull
    private NegativeScenarioResult mqttPublishDisconnected() {
        mqtt.disconnect();
        boolean sent = mqtt.publish(ProductionIotConstants.DEVICE_MESSAGES_TOPIC + "neg", "{}");
        if (!sent) {
            logger.error(ErrorCode.MQTT_PUBLISH.code, "publish rejected while disconnected");
            return NegativeScenarioResult.pass("MQTT_PUBLISH_DISCONNECTED", ErrorCode.MQTT_NOT_CONNECTED,
                    "publish correctly skipped");
        }
        return NegativeScenarioResult.fail("MQTT_PUBLISH_DISCONNECTED", ErrorCode.MQTT_PUBLISH,
                "publish succeeded while disconnected");
    }

    @NonNull
    private NegativeScenarioResult mqttSubscribeDisconnected() {
        mqtt.disconnect();
        boolean ok = wait(mqtt.subscribe("cloudMessages/neg", 1), 5);
        if (!ok) {
            logger.error(ErrorCode.MQTT_SUBSCRIBE.code, "subscribe rejected while disconnected");
            return NegativeScenarioResult.pass("MQTT_SUBSCRIBE_DISCONNECTED", ErrorCode.MQTT_NOT_CONNECTED,
                    "subscribe correctly skipped");
        }
        return NegativeScenarioResult.fail("MQTT_SUBSCRIBE_DISCONNECTED", ErrorCode.MQTT_SUBSCRIBE,
                "subscribe succeeded while disconnected");
    }

    @NonNull
    private NegativeScenarioResult mqttInvalidCredentials() {
        mqtt.disconnect();
        boolean connected = wait(mqtt.connect(new CompanionMqttClient.MqttConnectParams(
                ProductionIotConstants.defaultEndpointForEnv(env.environment()),
                "neg-invalid-client",
                ProductionIotConstants.DEFAULT_CUSTOM_AUTHORIZER,
                "not-a-jwt"
        )), 12);
        mqtt.disconnect();
        if (!connected) {
            logger.error(ErrorCode.MQTT_AUTH.code, "connect rejected with invalid JWT");
            return NegativeScenarioResult.pass("MQTT_INVALID_CREDS", ErrorCode.MQTT_AUTH, "connect failed as expected");
        }
        return NegativeScenarioResult.fail("MQTT_INVALID_CREDS", ErrorCode.MQTT_AUTH, "connect succeeded with junk JWT");
    }

    @NonNull
    private NegativeScenarioResult mqttConnectTimeout() {
        mqtt.disconnect();
        boolean connected = wait(mqtt.connect(new CompanionMqttClient.MqttConnectParams(
                "this-host-does-not-exist.invalid",
                "neg-timeout-client",
                ProductionIotConstants.DEFAULT_CUSTOM_AUTHORIZER,
                expiredJwt()
        )), 6);
        mqtt.disconnect();
        if (!connected) {
            logger.error(ErrorCode.MQTT_TIMEOUT.code, "connect to invalid host failed/timed out");
            return NegativeScenarioResult.pass("MQTT_CONNECT_TIMEOUT", ErrorCode.MQTT_TIMEOUT, "timed out as expected");
        }
        return NegativeScenarioResult.fail("MQTT_CONNECT_TIMEOUT", ErrorCode.MQTT_TIMEOUT, "connected to invalid host");
    }

    @NonNull
    private NegativeScenarioResult noInternetProbe() {
        NetworkInspector.ProbeResult probe = network.probeUnreachable();
        if (!probe.ok) {
            logger.error(ErrorCode.NET_TIMEOUT.code, "blackhole 192.0.2.1 failed in " + probe.latencyMs + "ms");
            return NegativeScenarioResult.pass("NETWORK_NO_INTERNET", ErrorCode.NET_TIMEOUT,
                    probe.detail + " rtt=" + probe.latencyMs + "ms");
        }
        return NegativeScenarioResult.fail("NETWORK_NO_INTERNET", ErrorCode.NET_UNAVAILABLE,
                "blackhole address was reachable");
    }

    @NonNull
    private NegativeScenarioResult wifiInterruptAndRecover() {
        NetworkInspector.NetworkSnapshot before = network.snapshot();
        NetworkInspector.WifiToggleResult off = network.setWifiEnabled(false);
        if (!off.applied) {
            logger.error(ErrorCode.NET_TOGGLE_DENIED.code, off.detail);
            return NegativeScenarioResult.skipped("NETWORK_WIFI_INTERRUPT", ErrorCode.NET_TOGGLE_DENIED, off.detail);
        }
        sleep(2500);
        NetworkInspector.NetworkSnapshot down = network.snapshot();
        NetworkInspector.WifiToggleResult on = network.setWifiEnabled(true);
        sleep(3500);
        NetworkInspector.NetworkSnapshot after = network.snapshot();
        boolean interrupted = !down.httpsReachable || !down.wifiEnabled;
        boolean recovered = after.wifiEnabled || after.httpsReachable || after.connected;
        if (interrupted && recovered) {
            logger.pass("Wi-Fi interrupt then recovery observed");
            return NegativeScenarioResult.pass("NETWORK_WIFI_INTERRUPT", ErrorCode.NET_WIFI_OFF,
                    "before=" + before.summary() + " down=" + down.summary() + " after=" + after.summary());
        }
        if (!on.applied) {
            return NegativeScenarioResult.fail("NETWORK_WIFI_INTERRUPT", ErrorCode.NET_WIFI_OFF,
                    "disabled wifi but could not re-enable: " + on.detail);
        }
        return NegativeScenarioResult.fail("NETWORK_WIFI_INTERRUPT", ErrorCode.NET_WIFI_OFF,
                "toggle applied but interrupt/recovery not observed down=" + down.summary());
    }

    private void restore(String saved) {
        if (saved != null) {
            tokenStore.save(saved);
        } else {
            tokenStore.clear();
        }
    }

    private boolean wait(@NonNull CompletableFuture<Boolean> future, int seconds) {
        try {
            return Boolean.TRUE.equals(future.get(seconds, TimeUnit.SECONDS));
        } catch (Exception e) {
            return false;
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @NonNull
    static String expiredJwt() {
        String header = b64("{\"alg\":\"none\",\"typ\":\"JWT\"}");
        String payload = b64("{\"exp\":1,\"deviceId\":\"neg-expired\"}");
        return header + "." + payload + ".x";
    }

    @NonNull
    private static String b64(@NonNull String json) {
        return Base64.encodeToString(json.getBytes(StandardCharsets.UTF_8), Base64.URL_SAFE | Base64.NO_WRAP);
    }
}
