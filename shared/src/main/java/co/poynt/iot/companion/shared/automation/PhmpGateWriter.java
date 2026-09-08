package co.poynt.iot.companion.shared.automation;

import android.content.Context;
import android.content.pm.PackageInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import co.poynt.iot.companion.shared.iot.IotStatusSnapshot;
import co.poynt.iot.companion.shared.iot.phase2.IotActionResult;
import co.poynt.iot.companion.shared.iot.phase2.IotRuntimeState;

/**
 * Combined PHMP gate file. Shape matches {@code ValidationResult} enough for a host evaluator:
 * {@code status}, {@code passed}, {@code skipped}, {@code message}, {@code details}, {@code metrics}.
 * Never includes a raw GD token.
 */
public final class PhmpGateWriter {

    private final File dir;
    private final String packageName;
    private final String companionVersion;

    public PhmpGateWriter(@NonNull Context context) {
        File external = context.getExternalFilesDir(null);
        this.dir = external != null ? external : context.getFilesDir();
        this.packageName = context.getPackageName();
        this.companionVersion = versionName(context);
    }

    @Nullable
    public String write(
            @NonNull IotRuntimeState state,
            @NonNull IotActionResult flow,
            @NonNull IotActionResult negatives) {
        try {
            if (!dir.exists() && !dir.mkdirs()) {
                return null;
            }
            boolean pass = flow.pass && negatives.pass;
            JSONObject json = new JSONObject();
            json.put("schemaVersion", AutomationContract.SCHEMA_VERSION);
            json.put("checkName", AutomationContract.CHECK_NAME);
            json.put("packageName", packageName);
            json.put("companionVersion", companionVersion);
            json.put("generatedAt", isoNow());
            json.put("passed", pass);
            json.put("skipped", false);
            json.put("status", pass ? "PASS" : "FAIL");
            json.put("message", pass
                    ? "Full IoT flow and negative suite both PASS (SKIPPED negatives allowed)"
                    : "IoT companion gate FAIL — flow=" + flow.status + " negatives=" + negatives.status);
            JSONArray details = new JSONArray();
            details.put("flow=" + flow);
            details.put("negatives=" + negatives);
            details.put("eligibility=" + state.eligibility);
            details.put("discover=" + state.discover);
            details.put("token=" + state.gdToken);
            details.put("mqtt=" + state.mqtt);
            details.put("subscription=" + state.subscription);
            details.put("publish=" + state.publish);
            details.put("receive=" + state.receive);
            details.put("reconnect=" + state.reconnect);
            details.put("overall=" + state.overall);
            details.put("negativeSummary=" + state.negativeSummary);
            details.put("lastErrorCode=" + state.lastErrorCode);
            details.put("tokenFingerprint=" + state.tokenFingerprint);
            json.put("details", details);
            JSONObject metrics = new JSONObject();
            metrics.put("eligibility", status(state.eligibility));
            metrics.put("discover", status(state.discover));
            metrics.put("token", status(state.gdToken));
            metrics.put("mqttConnection", status(state.mqtt));
            metrics.put("subscription", status(state.subscription));
            metrics.put("messagePublish", status(state.publish));
            metrics.put("messageReceive", status(state.receive));
            metrics.put("reconnect", status(state.reconnect));
            metrics.put("iotOverall", status(state.overall));
            metrics.put("flowPass", flow.pass);
            metrics.put("negativesPass", negatives.pass);
            metrics.put("negativeSummary", state.negativeSummary == null ? "—" : state.negativeSummary);
            json.put("metrics", metrics);
            json.put("files", new JSONObject()
                    .put("result", AutomationContract.RESULT_FILE)
                    .put("diagnostics", AutomationContract.DIAGNOSTICS_FILE)
                    .put("negative", AutomationContract.NEGATIVE_FILE)
                    .put("evidence", AutomationContract.EVIDENCE_FILE)
                    .put("gate", AutomationContract.GATE_FILE)
                    .put("release", AutomationContract.RELEASE_FILE));
            File file = new File(dir, AutomationContract.GATE_FILE);
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                writer.write(json.toString(2));
            }
            return file.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    @NonNull
    private static String status(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            return IotStatusSnapshot.UNKNOWN;
        }
        return value;
    }

    @NonNull
    private static String versionName(@NonNull Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionName == null ? "unknown" : info.versionName;
        } catch (Exception e) {
            return "unknown";
        }
    }

    @NonNull
    private static String isoNow() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(new Date());
    }
}
