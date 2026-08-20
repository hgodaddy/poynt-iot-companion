package co.poynt.iot.companion.shared.diagnostics;

import androidx.annotation.NonNull;

/**
 * Stable QA error codes for evidence and PHMP. Never include secrets in {@code detail}.
 */
public enum ErrorCode {
    NONE("IOT-000", "No error"),
    NET_UNAVAILABLE("IOT-NET-001", "No validated network path"),
    NET_WIFI_OFF("IOT-NET-002", "Wi-Fi disabled"),
    NET_TIMEOUT("IOT-NET-003", "Network probe timed out"),
    NET_TOGGLE_DENIED("IOT-NET-004", "App cannot toggle Wi-Fi on this build"),
    DISCOVER_HTTP("IOT-DSC-001", "Discover HTTP failure"),
    DISCOVER_TIMEOUT("IOT-DSC-002", "Discover timed out"),
    DISCOVER_INVALID("IOT-DSC-003", "Discover response invalid"),
    DISCOVER_DUPLICATE("IOT-DSC-004", "Discover already in flight"),
    TOKEN_MISSING("IOT-TOK-001", "GD token missing"),
    TOKEN_EXPIRED("IOT-TOK-002", "GD token expired"),
    TOKEN_INVALID("IOT-TOK-003", "GD token not a usable JWT"),
    MQTT_CONNECT("IOT-MQTT-001", "MQTT connect failed"),
    MQTT_TIMEOUT("IOT-MQTT-002", "MQTT connect timed out"),
    MQTT_AUTH("IOT-MQTT-003", "MQTT authentication rejected"),
    MQTT_SUBSCRIBE("IOT-MQTT-004", "MQTT subscribe failed"),
    MQTT_PUBLISH("IOT-MQTT-005", "MQTT publish failed"),
    MQTT_NOT_CONNECTED("IOT-MQTT-006", "MQTT not connected");

    public final String code;
    public final String title;

    ErrorCode(String code, String title) {
        this.code = code;
        this.title = title;
    }

    @NonNull
    public String format(@NonNull String detail) {
        return code + " " + title + " — " + detail;
    }
}
