package co.poynt.cloudmessaging.iot.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import co.poynt.cloudmessaging.iot.test.ui.DashboardActivity;
import co.poynt.iot.companion.shared.automation.AutomationContract;

/**
 * UIAutomator smoke: dashboard and action buttons exist. Does not run MQTT or PHMP_GATE.
 */
@RunWith(AndroidJUnit4.class)
public class DashboardIdsTest {

    private static final long WAIT_MS = 8_000L;

    @Rule
    public ActivityScenarioRule<DashboardActivity> activityRule =
            new ActivityScenarioRule<>(DashboardActivity.class);

    @Test
    public void dashboardExposesStableResourceIds() {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        String pkg = AutomationContract.PACKAGE;
        assertTrue(device.wait(Until.hasObject(By.res(pkg, "dashboard_root")), WAIT_MS));
        String[] ids = {
                "btn_refresh",
                "btn_check_eligibility",
                "btn_run_discover",
                "btn_refresh_token",
                "btn_connect_mqtt",
                "btn_disconnect_mqtt",
                "btn_reconnect",
                "btn_subscribe",
                "btn_publish",
                "btn_full_flow",
                "btn_diagnostics",
                "btn_neg_token",
                "btn_neg_discover",
                "btn_neg_mqtt",
                "btn_neg_network",
                "btn_neg_suite",
                "btn_phmp_gate",
                "btn_collect_logs",
                "value_eligibility",
                "value_overall",
                "evidence_log"
        };
        for (String id : ids) {
            assertNotNull("missing " + id, device.findObject(By.res(pkg, id)));
        }
        device.findObject(By.res(pkg, "btn_refresh")).click();
        assertTrue(device.wait(Until.hasObject(By.res(pkg, "dashboard_root")), WAIT_MS));
    }
}
