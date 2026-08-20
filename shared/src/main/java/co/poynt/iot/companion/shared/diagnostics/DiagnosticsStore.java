package co.poynt.iot.companion.shared.diagnostics;

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

import co.poynt.iot.companion.shared.iot.phase2.IotRuntimeState;
import co.poynt.iot.companion.shared.logging.EvidenceLogger;

/**
 * Persists diagnostics, evidence, and negative-test results for ADB pull / PHMP.
 */
public final class DiagnosticsStore implements EvidenceLogger.PersistSink {

    public static final String DIAGNOSTICS_FILE = "iot-diagnostics.json";
    public static final String EVIDENCE_FILE = "iot-companion-evidence.txt";

    private final File dir;

    public DiagnosticsStore(@NonNull Context context) {
        File external = context.getExternalFilesDir(null);
        this.dir = external != null ? external : context.getFilesDir();
    }

    @NonNull
    public File directory() {
        return dir;
    }

    @Override
    public void onLine(@NonNull String line) {
        append(new File(dir, EVIDENCE_FILE), line + "\n");
    }

    @Nullable
    public String writeDiagnostics(@NonNull DiagnosticSnapshot snapshot, @NonNull IotRuntimeState state) {
        try {
            ensureDir();
            JSONObject json = new JSONObject();
            json.put("generatedAt", isoNow());
            json.put("network", snapshot.networkSummary);
            json.put("wifiEnabled", snapshot.wifiEnabled);
            json.put("httpsReachable", snapshot.httpsReachable);
            json.put("latencyMs", snapshot.latencyMs);
            json.put("transport", snapshot.transport);
            json.put("mqttState", snapshot.mqttState);
            json.put("mqttAttempts", snapshot.mqttAttempts);
            json.put("mqttLastError", snapshot.mqttLastError);
            json.put("tokenState", snapshot.tokenState);
            json.put("tokenFingerprint", snapshot.tokenFingerprint);
            json.put("lastErrorCode", snapshot.lastErrorCode);
            json.put("lastErrorDetail", snapshot.lastErrorDetail);
            json.put("clientId", state.clientId);
            json.put("iotEndpoint", state.iotEndpoint);
            File file = new File(dir, DIAGNOSTICS_FILE);
            writeUtf8(file, json.toString(2));
            writeUtf8(new File(dir, stamp("diagnostics") + ".json"), json.toString(2));
            return file.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    private void ensureDir() {
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private static void writeUtf8(@NonNull File file, @NonNull String content) throws Exception {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    private static void append(@NonNull File file, @NonNull String content) {
        try {
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8)) {
                writer.write(content);
            }
        } catch (Exception ignored) {
        }
    }

    @NonNull
    private static String isoNow() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(new Date());
    }

    @NonNull
    private static String stamp(@NonNull String prefix) {
        return prefix + "-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
    }
}
