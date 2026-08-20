package co.poynt.iot.companion.shared.iot.phase2;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import co.poynt.iot.companion.shared.logging.EvidenceLogger;

/**
 * Machine-readable result for PHMP / ADB pull. Never includes the raw GD token.
 */
public final class JsonResultWriter {

    public static final String FILE_NAME = "iot-companion-result.json";
    public static final String LOG_NAME = "iot-companion-evidence.txt";

    private final File dir;

    public JsonResultWriter(@NonNull Context context) {
        File external = context.getExternalFilesDir(null);
        this.dir = external != null ? external : context.getFilesDir();
    }

    @Nullable
    public String write(@NonNull IotRuntimeState state, @NonNull EvidenceLogger logger) {
        try {
            if (!dir.exists() && !dir.mkdirs()) {
                return null;
            }
            File jsonFile = new File(dir, FILE_NAME);
            JSONObject json = new JSONObject();
            json.put("generatedAt", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(new Date()));
            json.put("clientId", state.clientId);
            json.put("iotEnabled", state.iotEnabled);
            json.put("eligibility", state.eligibility);
            json.put("discover", state.discover);
            json.put("token", state.gdToken);
            json.put("tokenFingerprint", state.tokenFingerprint);
            json.put("mqttConnection", state.mqtt);
            json.put("subscription", state.subscription);
            json.put("messagePublish", state.publish);
            json.put("messageReceive", state.receive);
            json.put("reconnect", state.reconnect);
            json.put("overall", state.overall);
            json.put("iotEndpoint", state.iotEndpoint);
            json.put("mothershipUrl", state.mothershipUrl);
            json.put("lastCorrelationId", state.lastCorrelationId);
            json.put("lastMessageAt", state.lastMessageAt);
            json.put("mqttState", state.mqttState);
            json.put("mqttAttempts", state.mqttAttempts);
            json.put("mqttLastError", state.mqttLastError);
            json.put("tokenState", state.tokenState);
            json.put("lastErrorCode", state.lastErrorCode);
            json.put("lastErrorDetail", state.lastErrorDetail);
            json.put("networkSummary", state.networkSummary);
            json.put("negativeSummary", state.negativeSummary);
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(jsonFile), StandardCharsets.UTF_8)) {
                writer.write(json.toString(2));
            }
            File logFile = new File(dir, LOG_NAME);
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(logFile), StandardCharsets.UTF_8)) {
                writer.write(logger.asText());
            }
            return jsonFile.getAbsolutePath();
        } catch (Exception e) {
            logger.fail("JSON export failed: " + e.getMessage());
            return null;
        }
    }
}
