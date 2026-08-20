package co.poynt.cloudmessaging.iot.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import co.poynt.iot.companion.shared.config.EnvironmentConfig;
import co.poynt.iot.companion.shared.iot.phase2.GdTokenStore;
import co.poynt.iot.companion.shared.iot.phase2.JwtInspector;
import co.poynt.iot.companion.shared.logging.EvidenceLogger;

/**
 * Lab/automation injection:
 * {@code adb shell am broadcast -a co.poynt.cloudmessaging.iot.test.SET_GD_TOKEN --es token '<jwt>'}
 */
public class TokenInjectReceiver extends BroadcastReceiver {

    public static final String ACTION = "co.poynt.cloudmessaging.iot.test.SET_GD_TOKEN";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION.equals(intent.getAction())) {
            return;
        }
        String token = intent.getStringExtra("token");
        EvidenceLogger logger = new EvidenceLogger();
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
}
