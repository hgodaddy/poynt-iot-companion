package co.poynt.iot.companion.shared.iot.phase2;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import co.poynt.iot.companion.shared.config.EnvironmentConfig;
import co.poynt.iot.companion.shared.device.DeviceInspector;
import co.poynt.iot.companion.shared.device.DeviceSnapshot;
import co.poynt.iot.companion.shared.logging.EvidenceLogger;
import co.poynt.iot.companion.shared.pcm.ProductionAppInspector;
import co.poynt.iot.companion.shared.pcm.ProductionAppSnapshot;
import co.poynt.iot.companion.shared.iot.IotStatusSnapshot;

/**
 * Independent IoT test actions plus full-flow orchestration. Stops the full flow on first FAIL.
 */
public final class IotController implements CompanionMqttClient.Listener {

    public interface Listener {
        void onUpdated();

        void onComplete(@NonNull IotActionResult result);
    }

    private final Context appContext;
    private final EvidenceLogger logger;
    private final EnvironmentConfig env;
    private final DeviceInspector deviceInspector = new DeviceInspector();
    private final ProductionAppInspector productionInspector = new ProductionAppInspector();
    private final DiscoverClient discoverClient;
    private final GdTokenStore tokenStore;
    private final CompanionMqttClient mqtt;
    private final JsonResultWriter jsonWriter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "IotController");
        t.setDaemon(true);
        return t;
    });

    private final IotRuntimeState state = new IotRuntimeState();
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private volatile Listener listener;
    private volatile CountDownLatch receiveLatch;

    public IotController(@NonNull Context context, @NonNull EvidenceLogger logger) {
        this.appContext = context.getApplicationContext();
        this.logger = logger;
        this.env = new EnvironmentConfig(appContext);
        this.discoverClient = new DiscoverClient(logger);
        this.tokenStore = new GdTokenStore(appContext, logger);
        this.mqtt = new CompanionMqttClient(logger);
        this.jsonWriter = new JsonResultWriter(appContext);
        this.mqtt.setListener(this);
        seedFromDevice();
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @NonNull
    public IotRuntimeState state() {
        return state;
    }

    public boolean isBusy() {
        return busy.get();
    }

    public void execute(@NonNull IotAction action) {
        if (!busy.compareAndSet(false, true)) {
            logger.info("Ignored " + action + " — another action is running");
            notifyUpdated();
            return;
        }
        executor.execute(() -> {
            try {
                IotActionResult result = action == IotAction.FULL_FLOW
                        ? runFullFlow()
                        : runSingle(action);
                state.computeOverall();
                notifyComplete(result);
            } finally {
                busy.set(false);
                notifyUpdated();
            }
        });
    }

    @NonNull
    private IotActionResult runSingle(@NonNull IotAction action) {
        switch (action) {
            case ELIGIBILITY:
                return checkEligibility();
            case DISCOVER:
                return runDiscover();
            case TOKEN:
                return refreshToken();
            case MQTT_CONNECT:
                return connectMqtt();
            case SUBSCRIBE:
                return subscribe();
            case PUBLISH:
                return publishAndAwait();
            case RECEIVE:
                return awaitReceive();
            case DISCONNECT:
                return disconnect();
            case RECONNECT:
                return reconnect();
            case EXPORT:
                return exportEvidence();
            default:
                return new IotActionResult(action, false, "Unknown action");
        }
    }

    @NonNull
    private IotActionResult runFullFlow() {
        logger.info("FULL IoT FLOW start");
        IotAction[] steps = {
                IotAction.ELIGIBILITY,
                IotAction.DISCOVER,
                IotAction.TOKEN,
                IotAction.MQTT_CONNECT,
                IotAction.SUBSCRIBE,
                IotAction.PUBLISH,
                IotAction.DISCONNECT,
                IotAction.RECONNECT,
                IotAction.EXPORT
        };
        IotActionResult last = new IotActionResult(IotAction.FULL_FLOW, false, "not started");
        for (IotAction step : steps) {
            last = runSingle(step);
            notifyUpdated();
            if (!last.pass) {
                logger.fail("FULL FLOW stopped at " + step + " — " + last.detail);
                state.overall = IotStatusSnapshot.FAIL;
                jsonWriter.write(state, logger);
                return new IotActionResult(IotAction.FULL_FLOW, false, "Stopped at " + step + ": " + last.detail);
            }
        }
        logger.pass("FULL IoT FLOW PASS");
        state.overall = IotStatusSnapshot.PASS;
        return new IotActionResult(IotAction.FULL_FLOW, true, "All IoT steps passed");
    }

    @NonNull
    private IotActionResult checkEligibility() {
        DeviceSnapshot device = deviceInspector.inspect();
        ProductionAppSnapshot production = productionInspector.inspect(appContext);
        boolean p70 = ProductionIotConstants.IOT_SUPPORTED_MODELS.stream()
                .anyMatch(m -> m.equalsIgnoreCase(device.model) || m.equalsIgnoreCase(device.product));
        boolean pcmOk = production.installed && production.enabled;
        boolean prefsHint = device.iotEnabledHint || state.iotEnabled;
        boolean eligible = pcmOk && (p70 || prefsHint || device.looksLikePoyntTerminal());
        state.clientId = resolveClientId(device);
        state.eligibilityDetail = "pcm=" + production.installedLabel()
                + " model=" + device.displayModel()
                + " p70Listed=" + p70
                + " iotHint=" + device.iotEnabledLabel();
        if (eligible) {
            state.eligibility = IotStatusSnapshot.PASS;
            logger.pass("Eligibility = PASS — " + state.eligibilityDetail);
            return new IotActionResult(IotAction.ELIGIBILITY, true, state.eligibilityDetail);
        }
        state.eligibility = IotStatusSnapshot.FAIL;
        logger.fail("Eligibility = FAIL — " + state.eligibilityDetail);
        return new IotActionResult(IotAction.ELIGIBILITY, false, state.eligibilityDetail);
    }

    @NonNull
    private IotActionResult runDiscover() {
        DeviceSnapshot device = deviceInspector.inspect();
        String clientId = resolveClientId(device);
        DiscoverClient.DiscoverResult result = discoverClient.discover(env.environment(), device.serial, clientId);
        state.mothershipUrl = result.mothershipUrl;
        state.discoverDetail = result.detail;
        state.iotEnabled = result.iotEnabled;
        if (result.services != null) {
            state.iotEndpoint = result.services.getIotEndpoint();
            if (result.services.getAuthorizerName() != null) {
                state.authorizer = result.services.getAuthorizerName();
            }
            if (result.services.getJobConfig() != null && result.services.getJobConfig().getMqttTopics() != null
                    && result.services.getJobConfig().getMqttTopics().getJobs() != null) {
                state.jobsTopic = result.services.getJobConfig().getMqttTopics().getJobs().getTopic();
            }
            env.setIotEndpoint(state.iotEndpoint);
        }
        state.discover = result.status;
        if (result.ok) {
            return new IotActionResult(IotAction.DISCOVER, true, result.detail);
        }
        return new IotActionResult(IotAction.DISCOVER, false, result.detail);
    }

    @NonNull
    private IotActionResult refreshToken() {
        String token = tokenStore.obtain();
        JwtInspector inspect = JwtInspector.inspect(token);
        state.tokenFingerprint = inspect.fingerprint;
        state.gdTokenDetail = inspect.summary;
        if (inspect.present && !inspect.expired) {
            if (!TextUtils.isEmpty(inspect.deviceId)) {
                state.clientId = inspect.deviceId;
            }
            state.gdToken = IotStatusSnapshot.PASS;
            logger.pass("GD token persisted in companion store " + inspect.summary);
            return new IotActionResult(IotAction.TOKEN, true, inspect.summary);
        }
        state.gdToken = IotStatusSnapshot.FAIL;
        return new IotActionResult(IotAction.TOKEN, false, inspect.summary
                + " — inject via adb: am broadcast -a co.poynt.cloudmessaging.iot.test.SET_GD_TOKEN --es token <jwt>");
    }

    @NonNull
    private IotActionResult connectMqtt() {
        String token = tokenStore.current();
        JwtInspector inspect = JwtInspector.inspect(token);
        if (!inspect.present || inspect.expired) {
            IotActionResult tokenResult = refreshToken();
            if (!tokenResult.pass) {
                state.mqtt = IotStatusSnapshot.FAIL;
                return new IotActionResult(IotAction.MQTT_CONNECT, false, "No valid GD token for MQTT auth");
            }
            token = tokenStore.current();
        }
        if (TextUtils.isEmpty(state.iotEndpoint) || "—".equals(state.iotEndpoint)) {
            state.iotEndpoint = env.iotEndpoint();
            if (state.iotEndpoint.startsWith("(")) {
                state.iotEndpoint = ProductionIotConstants.defaultEndpointForEnv(env.environment());
                logger.info("Using production default IoT endpoint for " + env.environment());
            }
        }
        DeviceSnapshot device = deviceInspector.inspect();
        if (TextUtils.isEmpty(state.clientId)) {
            state.clientId = resolveClientId(device);
        }
        boolean ok = waitFuture(mqtt.connect(new CompanionMqttClient.MqttConnectParams(
                state.iotEndpoint,
                state.clientId,
                state.authorizer,
                token
        )), 30);
        state.mqtt = ok ? IotStatusSnapshot.PASS : IotStatusSnapshot.FAIL;
        return new IotActionResult(IotAction.MQTT_CONNECT, ok, ok ? "CONNECTED" : "MQTT connect failed");
    }

    @NonNull
    private IotActionResult subscribe() {
        if (!mqtt.isConnected()) {
            IotActionResult connect = connectMqtt();
            if (!connect.pass) {
                state.subscription = IotStatusSnapshot.FAIL;
                return new IotActionResult(IotAction.SUBSCRIBE, false, "MQTT not connected");
            }
        }
        String cloud = ProductionIotConstants.CLOUD_MESSAGES_TOPIC + state.clientId;
        String device = ProductionIotConstants.DEVICE_MESSAGES_TOPIC + state.clientId;
        String jobs = TextUtils.isEmpty(state.jobsTopic)
                ? ProductionIotConstants.JOBS_TOPIC_PREFIX + state.clientId
                : state.jobsTopic.replace(ProductionIotConstants.TOPIC_DEVICE_ID_PLACEHOLDER, state.clientId);
        boolean cloudOk = waitFuture(mqtt.subscribe(cloud, ProductionIotConstants.DEFAULT_QOS), 15);
        boolean deviceOk = waitFuture(mqtt.subscribe(device, ProductionIotConstants.DEFAULT_QOS), 15);
        boolean jobsOk = waitFuture(mqtt.subscribe(jobs, ProductionIotConstants.DEFAULT_QOS), 15);
        boolean ok = cloudOk && deviceOk;
        state.subscription = ok ? IotStatusSnapshot.PASS : IotStatusSnapshot.FAIL;
        String detail = "cloud=" + cloudOk + " deviceLoopback=" + deviceOk + " jobs=" + jobsOk;
        return new IotActionResult(IotAction.SUBSCRIBE, ok, detail);
    }

    @NonNull
    private IotActionResult publishAndAwait() {
        if (!IotStatusSnapshot.PASS.equals(state.subscription)) {
            IotActionResult sub = subscribe();
            if (!sub.pass) {
                state.publish = IotStatusSnapshot.FAIL;
                return new IotActionResult(IotAction.PUBLISH, false, "Subscribe required before publish");
            }
        }
        String correlation = "cmp-" + UUID.randomUUID();
        state.lastCorrelationId = correlation;
        state.lastReceiveValidated = false;
        receiveLatch = new CountDownLatch(1);
        String topic = ProductionIotConstants.DEVICE_MESSAGES_TOPIC + state.clientId;
        JsonObject auth = new JsonObject();
        auth.addProperty("device_uuid", state.clientId);
        auth.addProperty("message", ProductionIotConstants.DEVICE_AUTHENTICATED_MESSAGE);
        mqtt.publish(topic, gson.toJson(auth));
        JsonObject test = new JsonObject();
        test.addProperty("type", "IOT_COMPANION_TEST");
        test.addProperty("correlationId", correlation);
        test.addProperty("deviceId", state.clientId);
        boolean sent = mqtt.publish(topic, gson.toJson(test));
        state.publish = sent ? IotStatusSnapshot.PASS : IotStatusSnapshot.FAIL;
        if (!sent) {
            return new IotActionResult(IotAction.PUBLISH, false, "Publish failed");
        }
        boolean received = awaitLatch(receiveLatch, 20);
        state.receive = state.lastReceiveValidated ? IotStatusSnapshot.PASS : IotStatusSnapshot.FAIL;
        boolean ok = sent && state.lastReceiveValidated;
        String detail = "published correlationId=" + correlation + " validated=" + state.lastReceiveValidated;
        if (!received) {
            detail = detail + " (no message within 20s — cloud inject may still be needed for cloudMessages/)";
        }
        return new IotActionResult(IotAction.PUBLISH, ok, detail);
    }

    @NonNull
    private IotActionResult awaitReceive() {
        receiveLatch = new CountDownLatch(1);
        boolean received = awaitLatch(receiveLatch, 20);
        if (received || state.lastReceiveValidated) {
            state.receive = IotStatusSnapshot.PASS;
            return new IotActionResult(IotAction.RECEIVE, true, "Message received at " + state.lastMessageAt);
        }
        state.receive = IotStatusSnapshot.FAIL;
        return new IotActionResult(IotAction.RECEIVE, false, "No MQTT message in 20s");
    }

    @NonNull
    private IotActionResult disconnect() {
        mqtt.disconnect();
        state.mqtt = "DISCONNECTED";
        return new IotActionResult(IotAction.DISCONNECT, true, "MQTT disconnect issued");
    }

    @NonNull
    private IotActionResult reconnect() {
        mqtt.disconnect();
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        IotActionResult connect = connectMqtt();
        if (!connect.pass) {
            state.reconnect = IotStatusSnapshot.FAIL;
            return new IotActionResult(IotAction.RECONNECT, false, "Reconnect connect failed");
        }
        IotActionResult sub = subscribe();
        boolean ok = sub.pass;
        state.reconnect = ok ? IotStatusSnapshot.PASS : IotStatusSnapshot.FAIL;
        return new IotActionResult(IotAction.RECONNECT, ok, ok ? "Reconnected and resubscribed" : sub.detail);
    }

    @NonNull
    private IotActionResult exportEvidence() {
        state.computeOverall();
        String path = jsonWriter.write(state, logger);
        logger.pass("Exported evidence " + path);
        return new IotActionResult(IotAction.EXPORT, path != null, path == null ? "export failed" : path);
    }

    @Override
    public void onConnect() {
        state.mqtt = IotStatusSnapshot.PASS;
        notifyUpdated();
    }

    @Override
    public void onDisconnect() {
        if (!IotStatusSnapshot.PASS.equals(state.reconnect)) {
            state.mqtt = "DISCONNECTED";
        }
        notifyUpdated();
    }

    @Override
    public void onError(@NonNull String error) {
        logger.fail(error);
        notifyUpdated();
    }

    @Override
    public void onMessage(@NonNull String topic, @NonNull String payload) {
        state.onMessage(topic, payload);
        CountDownLatch latch = receiveLatch;
        if (latch != null) {
            latch.countDown();
        }
        notifyUpdated();
    }

    private void seedFromDevice() {
        DeviceSnapshot device = deviceInspector.inspect();
        state.discoveryUrl = emptyToDash(DeviceInspector.systemProperty("persist.poynt.pcm.discovery", ""));
        String pcm = DeviceInspector.systemProperty("persist.poynt.srvc.url.pcm", "");
        if (pcm.isEmpty()) {
            pcm = DeviceInspector.systemProperty("persist.poynt.pcm.endpoint", "");
        }
        state.pcmEndpoint = emptyToDash(pcm);
        state.clientId = resolveClientId(device);
        JwtInspector stored = JwtInspector.inspect(tokenStore.current());
        if (stored.present) {
            state.gdToken = stored.expired ? IotStatusSnapshot.FAIL : IotStatusSnapshot.PASS;
            state.gdTokenDetail = stored.summary;
            state.tokenFingerprint = stored.fingerprint;
        }
    }

    @NonNull
    private String resolveClientId(@NonNull DeviceSnapshot device) {
        if (!TextUtils.isEmpty(state.clientId) && !"UNAVAILABLE".equals(state.clientId)) {
            return state.clientId;
        }
        JwtInspector inspect = JwtInspector.inspect(tokenStore.current());
        if (!TextUtils.isEmpty(inspect.deviceId)) {
            return inspect.deviceId;
        }
        String fromProp = DeviceInspector.systemProperty("persist.poynt.device.id", "");
        if (TextUtils.isEmpty(fromProp)) {
            fromProp = DeviceInspector.systemProperty("ro.poynt.device.id", "");
        }
        if (!TextUtils.isEmpty(fromProp)) {
            return fromProp;
        }
        return device.serial;
    }

    private boolean waitFuture(@NonNull CompletableFuture<Boolean> future, int seconds) {
        try {
            Boolean value = future.get(seconds, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(value);
        } catch (Exception e) {
            logger.fail("Timed out after " + seconds + "s: " + e.getMessage());
            return false;
        }
    }

    private boolean awaitLatch(@Nullable CountDownLatch latch, int seconds) {
        if (latch == null) {
            return false;
        }
        try {
            return latch.await(seconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @NonNull
    private static String emptyToDash(@NonNull String value) {
        return value.isEmpty() ? "—" : value;
    }

    private void notifyUpdated() {
        Listener l = listener;
        if (l != null) {
            l.onUpdated();
        }
    }

    private void notifyComplete(@NonNull IotActionResult result) {
        Listener l = listener;
        if (l != null) {
            l.onComplete(result);
        }
    }
}
