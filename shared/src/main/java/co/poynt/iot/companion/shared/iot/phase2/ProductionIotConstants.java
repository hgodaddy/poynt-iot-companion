package co.poynt.iot.companion.shared.iot.phase2;

import java.util.Arrays;
import java.util.List;

/**
 * Production {@code IoTConstants} values used by the companion MQTT/Discover path.
 * AWS endpoints here are the same environment defaults the production app uses when
 * Discover has not yet returned an endpoint — not AWS access keys.
 */
public final class ProductionIotConstants {

    private ProductionIotConstants() {}

    public static final List<String> IOT_SUPPORTED_MODELS = Arrays.asList("P70");

    public static final String DEFAULT_CUSTOM_AUTHORIZER = "mothership-iot-authorizer";
    public static final String DEFAULT_TOKEN_KEY_NAME = "token";

    public static final String CLOUD_MESSAGES_TOPIC = "cloudMessages/";
    public static final String DEVICE_MESSAGES_TOPIC = "deviceMessages/";
    public static final String JOBS_TOPIC_PREFIX = "jobs/";
    public static final String MAINTENANCE_TOPIC_PREFIX = "maintenance/";
    public static final String TOPIC_DEVICE_ID_PLACEHOLDER = "{deviceId}";

    public static final String DEVICE_AUTHENTICATED_MESSAGE = "DEVICE_AUTHENTICATED";
    public static final int DEFAULT_QOS = 1;
    public static final long SESSION_EXPIRY_INTERVAL_SECONDS = 86400L;

    public static final String FOURONEONE_LIVE = "https://fouroneone.poynt.net/discovery/services";
    public static final String FOURONEONE_CI = "https://fouroneone-ci.poynt.net/discovery/services";

    public static String defaultEndpointForEnv(String environment) {
        if ("DEV".equalsIgnoreCase(environment) || "CI".equalsIgnoreCase(environment)) {
            return "a20kb7gwzbzv4b-ats.iot.us-east-1.amazonaws.com";
        }
        if ("OTE".equalsIgnoreCase(environment) || "STAGING".equalsIgnoreCase(environment)) {
            return "a4mu1u3wsbsil-ats.iot.us-east-1.amazonaws.com";
        }
        return "a20kb7gwzbzv4b-ats.iot.us-east-1.amazonaws.com";
    }

    public static boolean isValidEndpoint(String endpoint) {
        if (endpoint == null || endpoint.trim().isEmpty()) {
            return false;
        }
        String trimmed = endpoint.trim();
        return trimmed.matches("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
                && trimmed.endsWith(".amazonaws.com");
    }
}
