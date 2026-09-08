package co.poynt.cloudmessaging.iot.test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import co.poynt.cloudmessaging.iot.test.ui.DashboardActivity;

/**
 * Instrumented smoke: dashboard and action buttons exist. Does not run MQTT or RELEASE_GATE.
 */
@RunWith(AndroidJUnit4.class)
public class DashboardIdsTest {

    @Rule
    public ActivityScenarioRule<DashboardActivity> activityRule =
            new ActivityScenarioRule<>(DashboardActivity.class);

    @Test
    public void dashboardExposesStableResourceIds() {
        onView(withId(R.id.dashboard_root)).check(matches(isDisplayed()));
        int[] ids = {
                R.id.btn_refresh,
                R.id.btn_check_eligibility,
                R.id.btn_run_discover,
                R.id.btn_refresh_token,
                R.id.btn_connect_mqtt,
                R.id.btn_disconnect_mqtt,
                R.id.btn_reconnect,
                R.id.btn_subscribe,
                R.id.btn_publish,
                R.id.btn_full_flow,
                R.id.btn_diagnostics,
                R.id.btn_neg_token,
                R.id.btn_neg_discover,
                R.id.btn_neg_mqtt,
                R.id.btn_neg_network,
                R.id.btn_neg_suite,
                R.id.btn_phmp_gate,
                R.id.btn_release_gate,
                R.id.btn_collect_logs,
                R.id.value_eligibility,
                R.id.value_overall,
                R.id.value_release,
                R.id.evidence_log
        };
        for (int id : ids) {
            onView(withId(id)).perform(scrollTo()).check(matches(isDisplayed()));
        }
        onView(withId(R.id.btn_refresh)).perform(scrollTo(), click());
        onView(withId(R.id.dashboard_root)).check(matches(isDisplayed()));
    }
}
