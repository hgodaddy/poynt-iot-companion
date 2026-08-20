package co.poynt.iot.companion.shared.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;

/**
 * Environment selection only. Endpoints and credentials are loaded externally —
 * never hard-code AWS keys, certificates, or production secrets.
 */
public final class EnvironmentConfig {

    public static final String PREFS = "iot_companion_config";
    public static final String KEY_ENV = "environment";
    public static final String ENV_DEV = "DEV";
    public static final String ENV_OTE = "OTE";
    public static final String ENV_PROD = "PROD";

    public static final String KEY_IOT_ENDPOINT = "iot_endpoint";

    private final SharedPreferences prefs;

    public EnvironmentConfig(@NonNull Context context) {
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @NonNull
    public String environment() {
        return prefs.getString(KEY_ENV, ENV_OTE);
    }

    public void setEnvironment(@NonNull String environment) {
        prefs.edit().putString(KEY_ENV, environment).apply();
    }

    @NonNull
    public String iotEndpoint() {
        String value = prefs.getString(KEY_IOT_ENDPOINT, "");
        return TextUtils.isEmpty(value) ? "(not configured — set externally)" : value;
    }

    public void setIotEndpoint(@NonNull String endpoint) {
        prefs.edit().putString(KEY_IOT_ENDPOINT, endpoint).apply();
    }
}
