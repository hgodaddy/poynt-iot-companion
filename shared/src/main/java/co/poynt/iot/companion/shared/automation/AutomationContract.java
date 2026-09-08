package co.poynt.iot.companion.shared.automation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import co.poynt.iot.companion.shared.iot.phase2.IotAction;

/**
 * Stable ADB / Appium / UIAutomator / PHMP contract. Do not rename without updating
 * {@code automation/ids.json} and {@code scripts/automation/}.
 */
public final class AutomationContract {

    public static final String PACKAGE = "co.poynt.cloudmessaging.iot.test";
    public static final String DASHBOARD_ACTIVITY = PACKAGE + ".ui.DashboardActivity";
    public static final String COMPONENT_DASHBOARD = PACKAGE + "/.ui.DashboardActivity";

    public static final String ACTION_DASHBOARD = PACKAGE + ".DASHBOARD";
    public static final String ACTION_SET_GD_TOKEN = PACKAGE + ".SET_GD_TOKEN";
    public static final String ACTION_RUN = PACKAGE + ".RUN_ACTION";

    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_TOKEN = "token";
    public static final String EXTRA_TIMEOUT_SEC = "timeoutSec";

    public static final String RESULT_FILE = "iot-companion-result.json";
    public static final String DIAGNOSTICS_FILE = "iot-diagnostics.json";
    public static final String NEGATIVE_FILE = "iot-negative-results.json";
    public static final String GATE_FILE = "iot-phmp-gate.json";
    public static final String EVIDENCE_FILE = "iot-companion-evidence.txt";

    public static final String SCHEMA_VERSION = "1.0";
    public static final String CHECK_NAME = "IoT Companion Gate";

    public static final String PRODUCTION_PACKAGE = "co.poynt.cloudmessaging";

    public static final int DEFAULT_TIMEOUT_SEC = 240;

    private AutomationContract() {
    }

    @NonNull
    public static String externalFilesGlob() {
        return "/sdcard/Android/data/" + PACKAGE + "/files";
    }

    @Nullable
    public static IotAction parseAction(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String name = raw.trim().toUpperCase().replace('-', '_');
        try {
            return IotAction.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
