package co.poynt.cloudmessaging.iot.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import co.poynt.iot.companion.shared.automation.AutomationContract;
import co.poynt.iot.companion.shared.config.EnvironmentConfig;
import co.poynt.iot.companion.shared.iot.phase2.GdTokenStore;
import co.poynt.iot.companion.shared.iot.phase2.JwtInspector;
import co.poynt.iot.companion.shared.logging.EvidenceLogger;

/**
 * Lab/automation injection:
 * {@code adb shell am broadcast -a co.poynt.cloudmessaging.iot.test.SET_GD_TOKEN --es token '<jwt>'}
 */
public class TokenInjectReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !AutomationContract.ACTION_SET_GD_TOKEN.equals(intent.getAction())) {
            return;
        }
        String token = intent.getStringExtra(AutomationContract.EXTRA_TOKEN);
        EvidenceLogger logger = logger(context);
        if (TextUtils.isEmpty(token)) {
            context.getSharedPreferences(EnvironmentConfig.PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .remove(GdTokenStore.KEY_GD_TOKEN)
                    .apply();
            logger.info("GD token cleared via broadcast");
            return;
        }
        new GdTokenStore(context, logger).save(token);
        JwtInspector inspect = JwtInspector.inspect(token);
        logger.pass("GD token injected via broadcast " + inspect.summary);
    }

    private static EvidenceLogger logger(Context context) {
        Context appCtx = context.getApplicationContext();
        if (appCtx instanceof CompanionApp) {
            return ((CompanionApp) appCtx).facade().logger();
        }
        return new EvidenceLogger();
    }
}
