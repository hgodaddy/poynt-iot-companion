package co.poynt.cloudmessaging.iot.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import co.poynt.iot.companion.shared.automation.AutomationContract;
import co.poynt.iot.companion.shared.iot.phase2.IotAction;
import co.poynt.iot.companion.shared.iot.phase2.IotActionResult;

/**
 * Headless ADB:
 * {@code adb shell am broadcast -a co.poynt.cloudmessaging.iot.test.RUN_ACTION --es action RELEASE_GATE --es buildId nightly --ei timeoutSec 360}
 */
public class AutomationReceiver extends BroadcastReceiver {

    private static final String TAG = "IotCompanionAuto";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !AutomationContract.ACTION_RUN.equals(intent.getAction())) {
            return;
        }
        Context appCtx = context.getApplicationContext();
        if (!(appCtx instanceof CompanionApp)) {
            Log.e(TAG, "RUN_ACTION ignored — application is not CompanionApp");
            return;
        }
        CompanionApp app = (CompanionApp) appCtx;
        IotAction action = AutomationContract.parseAction(intent.getStringExtra(AutomationContract.EXTRA_ACTION));
        if (action == null) {
            app.facade().logger().fail("RUN_ACTION missing or unknown action extra");
            return;
        }
        int timeoutSec = intent.getIntExtra(
                AutomationContract.EXTRA_TIMEOUT_SEC,
                action == IotAction.RELEASE_GATE
                        ? AutomationContract.RELEASE_TIMEOUT_SEC
                        : AutomationContract.DEFAULT_TIMEOUT_SEC);
        if (timeoutSec < 10) {
            timeoutSec = 10;
        }
        app.facade().iot().setBuildId(intent.getStringExtra(AutomationContract.EXTRA_BUILD_ID));
        PendingResult pending = goAsync();
        int timeoutMsCap = timeoutSec;
        new Thread(() -> {
            try {
                app.facade().logger().info("RUN_ACTION " + action + " timeoutSec=" + timeoutMsCap);
                IotActionResult result = app.facade().iot().executeBlocking(action, timeoutMsCap * 1000L);
                Log.i(TAG, result.toString());
            } catch (Exception e) {
                Log.e(TAG, "RUN_ACTION failed", e);
                app.facade().logger().fail("RUN_ACTION " + action + " " + e.getMessage());
            } finally {
                pending.finish();
            }
        }, "iot-run-action").start();
    }
}
