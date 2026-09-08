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

import co.poynt.iot.companion.shared.device.DeviceSnapshot;
import co.poynt.iot.companion.shared.iot.phase2.IotActionResult;
import co.poynt.iot.companion.shared.iot.phase2.IotRuntimeState;
import co.poynt.iot.companion.shared.pcm.ProductionAppSnapshot;

/**
 * Nightly release decision. Critical checks: production PCM, companion APK, IoT companion gate.
 * {@code releaseReady} is true only when every critical check passed and none were skipped.
 * Never includes a raw GD token.
 */
public final class ReleaseGateWriter {

    public static final String CHECK_PRODUCTION = "Production Cloud Messaging APK";
    public static final String CHECK_COMPANION = "Companion Test App";
    public static final String CHECK_DEVICE = "Device / PoyntOS";
    public static final String CHECK_IOT_GATE = AutomationContract.CHECK_NAME;

    private final File dir;
    private final String packageName;
    private final String companionVersion;

    public ReleaseGateWriter(@NonNull Context context) {
        File external = context.getExternalFilesDir(null);
        this.dir = external != null ? external : context.getFilesDir();
        this.packageName = context.getPackageName();
        this.companionVersion = versionName(context);
    }

    @Nullable
    public String write(
            @NonNull IotRuntimeState state,
            @NonNull ProductionAppSnapshot production,
            @NonNull DeviceSnapshot device,
            @NonNull IotActionResult phmpGate) {
        try {
            if (!dir.exists() && !dir.mkdirs()) {
                return null;
            }
            JSONArray checks = new JSONArray();
            JSONObject productionCheck = check(
                    CHECK_PRODUCTION,
                    production.installed && production.enabled,
                    false,
                    production.installedLabel()
                            + " pkg=" + production.packageName
                            + " v" + production.versionName
                            + " code=" + production.versionCode
                            + " priv=" + production.privileged
                            + " svc=" + production.serviceName);
            JSONObject companionCheck = check(
                    CHECK_COMPANION,
                    true,
                    false,
                    "pkg=" + packageName + " v" + companionVersion);
            JSONObject deviceCheck = check(
                    CHECK_DEVICE,
                    device.looksLikePoyntTerminal() || !device.poyntOsVersion.isEmpty(),
                    false,
                    "model=" + device.displayModel()
                            + " serial=" + device.serial
                            + " poyntOs=" + device.poyntOsVersion
                            + " android=" + device.androidRelease);
            JSONObject iotCheck = check(
                    CHECK_IOT_GATE,
                    phmpGate.pass,
                    false,
                    phmpGate.toString());
            checks.put(productionCheck);
            checks.put(companionCheck);
            checks.put(deviceCheck);
            checks.put(iotCheck);

            boolean anyFail = false;
            boolean anySkip = false;
            for (int i = 0; i < checks.length(); i++) {
                JSONObject item = checks.getJSONObject(i);
                if (item.optBoolean("skipped", false)) {
                    anySkip = true;
                } else if (!item.optBoolean("passed", false)) {
                    anyFail = true;
                }
            }
            boolean releaseReady = !anyFail && !anySkip;
            String status = anyFail ? "FAIL" : (anySkip ? "SKIP" : "PASS");
            String message;
            if (releaseReady) {
                message = "PASS — nightly IoT build is release-ready";
            } else if (anyFail) {
                message = "FAIL — IoT release blocked (production, device, or IoT companion gate)";
            } else {
                message = "SKIP — IoT release not certifiable (critical check unevaluated)";
            }

            JSONObject json = new JSONObject();
            json.put("schemaVersion", AutomationContract.SCHEMA_VERSION);
            json.put("checkName", AutomationContract.RELEASE_CHECK_NAME);
            json.put("packageName", packageName);
            json.put("companionVersion", companionVersion);
            json.put("productionPackage", production.packageName);
            json.put("productionVersion", production.versionName);
            json.put("buildId", state.buildId == null || state.buildId.isEmpty() ? "local" : state.buildId);
            json.put("generatedAt", isoNow());
            json.put("passed", releaseReady);
            json.put("skipped", anySkip && !anyFail);
            json.put("releaseReady", releaseReady);
            json.put("status", status);
            json.put("message", message);
            json.put("checks", checks);
            JSONArray details = new JSONArray();
            details.put("production=" + production.installedLabel());
            details.put("companion=" + companionVersion);
            details.put("device=" + device.displayModel());
            details.put("phmpGate=" + phmpGate);
            details.put("tokenFingerprint=" + state.tokenFingerprint);
            json.put("details", details);
            JSONObject metrics = new JSONObject();
            metrics.put("releaseReady", releaseReady);
            metrics.put("productionInstalled", production.installed);
            metrics.put("productionEnabled", production.enabled);
            metrics.put("flowAndNegativesPass", phmpGate.pass);
            metrics.put("iotOverall", state.overall);
            metrics.put("negativeSummary", state.negativeSummary == null ? "—" : state.negativeSummary);
            json.put("metrics", metrics);
            json.put("files", new JSONObject()
                    .put("result", AutomationContract.RESULT_FILE)
                    .put("diagnostics", AutomationContract.DIAGNOSTICS_FILE)
                    .put("negative", AutomationContract.NEGATIVE_FILE)
                    .put("evidence", AutomationContract.EVIDENCE_FILE)
                    .put("gate", AutomationContract.GATE_FILE)
                    .put("release", AutomationContract.RELEASE_FILE));
            File file = new File(dir, AutomationContract.RELEASE_FILE);
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                writer.write(json.toString(2));
            }
            return file.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    @NonNull
    private static JSONObject check(
            @NonNull String name,
            boolean passed,
            boolean skipped,
            @NonNull String message) throws Exception {
        JSONObject item = new JSONObject();
        item.put("name", name);
        item.put("passed", passed || skipped);
        item.put("skipped", skipped);
        item.put("status", skipped ? "SKIP" : (passed ? "PASS" : "FAIL"));
        item.put("message", message);
        item.put("critical", true);
        return item;
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
