package co.poynt.iot.companion.shared.iot;

import androidx.annotation.NonNull;

/**
 * Phase 1 can observe device + production APK state. Token / MQTT / subscribe require
 * in-process reuse of production IoT classes (Phase 2) because those live in another UID.
 */
public final class IotStatusSnapshot {

    public static final String PASS = "PASS";
    public static final String FAIL = "FAIL";
    public static final String UNKNOWN = "UNKNOWN";
    public static final String PENDING = "PENDING_PHASE_2";

    public final String eligibility;
    public final String eligibilityDetail;
    public final String discover;
    public final String discoverDetail;
    public final String gdToken;
    public final String mqtt;
    public final String subscription;
    public final String publish;
    public final String receive;
    public final String reconnect;
    public final String overall;
    public final String lastMessageAt;
    public final String discoveryUrl;
    public final String pcmEndpoint;
    public final String tokenDetail;
    public final String mothershipUrl;

    public IotStatusSnapshot(
            @NonNull String eligibility,
            @NonNull String eligibilityDetail,
            @NonNull String discover,
            @NonNull String discoverDetail,
            @NonNull String gdToken,
            @NonNull String mqtt,
            @NonNull String subscription,
            @NonNull String lastMessageAt,
            @NonNull String discoveryUrl,
            @NonNull String pcmEndpoint) {
        this(eligibility, eligibilityDetail, discover, discoverDetail, gdToken, mqtt, subscription,
                "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", lastMessageAt, discoveryUrl, pcmEndpoint, "", "—");
    }

    public IotStatusSnapshot(
            @NonNull String eligibility,
            @NonNull String eligibilityDetail,
            @NonNull String discover,
            @NonNull String discoverDetail,
            @NonNull String gdToken,
            @NonNull String mqtt,
            @NonNull String subscription,
            @NonNull String publish,
            @NonNull String receive,
            @NonNull String reconnect,
            @NonNull String overall,
            @NonNull String lastMessageAt,
            @NonNull String discoveryUrl,
            @NonNull String pcmEndpoint,
            @NonNull String tokenDetail,
            @NonNull String mothershipUrl) {
        this.eligibility = eligibility;
        this.eligibilityDetail = eligibilityDetail;
        this.discover = discover;
        this.discoverDetail = discoverDetail;
        this.gdToken = gdToken;
        this.mqtt = mqtt;
        this.subscription = subscription;
        this.publish = publish;
        this.receive = receive;
        this.reconnect = reconnect;
        this.overall = overall;
        this.lastMessageAt = lastMessageAt;
        this.discoveryUrl = discoveryUrl;
        this.pcmEndpoint = pcmEndpoint;
        this.tokenDetail = tokenDetail;
        this.mothershipUrl = mothershipUrl;
    }

    @NonNull
    public static IotStatusSnapshot fromRuntime(
            @NonNull co.poynt.iot.companion.shared.iot.phase2.IotRuntimeState runtime) {
        return new IotStatusSnapshot(
                status(runtime.eligibility),
                text(runtime.eligibilityDetail),
                status(runtime.discover),
                text(runtime.discoverDetail),
                status(runtime.gdToken),
                status(runtime.mqtt),
                status(runtime.subscription),
                status(runtime.publish),
                status(runtime.receive),
                status(runtime.reconnect),
                status(runtime.overall),
                text(runtime.lastMessageAt),
                text(runtime.discoveryUrl),
                text(runtime.pcmEndpoint),
                text(runtime.gdTokenDetail),
                text(runtime.mothershipUrl)
        );
    }

    @NonNull
    private static String status(String value) {
        return value == null || value.isEmpty() ? UNKNOWN : value;
    }

    @NonNull
    private static String text(String value) {
        return value == null || value.isEmpty() ? "—" : value;
    }
}
