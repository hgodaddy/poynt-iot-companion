package co.poynt.iot.companion.shared.device;

import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Reads terminal identity without talking to AWS or storing credentials.
 * Poynt terminals expose serial / OS version through {@code ro.*} and {@code persist.*} properties.
 */
public final class DeviceInspector {

    private static final List<String> SERIAL_PROPS = Arrays.asList(
            "ro.serialno",
            "ro.boot.serialno",
            "ro.poynt.serial",
            "persist.poynt.serial",
            "ril.serialnumber"
    );

    private static final List<String> POYNT_OS_PROPS = Arrays.asList(
            "ro.poynt.os.version",
            "ro.build.version.poynt",
            "ro.poynt.build.version",
            "ro.build.display.id"
    );

    private static final List<String> IOT_ENABLED_PROPS = Arrays.asList(
            "persist.poynt.iot.enabled",
            "ro.poynt.iot.enabled",
            "persist.poynt.cloud.iot.enabled",
            "persist.poynt.aws.iot.enabled"
    );

    @NonNull
    public DeviceSnapshot inspect() {
        String serial = firstProp(SERIAL_PROPS);
        if (TextUtils.isEmpty(serial)) {
            serial = safeBuildSerial();
        }

        String iotSource = "unset";
        boolean iotEnabled = false;
        for (String key : IOT_ENABLED_PROPS) {
            String value = systemProperty(key, "");
            if (!TextUtils.isEmpty(value)) {
                iotSource = key + "=" + value;
                iotEnabled = isTruthy(value);
                break;
            }
        }

        return new DeviceSnapshot(
                nullToEmpty(Build.MODEL),
                nullToEmpty(Build.MANUFACTURER),
                nullToEmpty(Build.DEVICE),
                TextUtils.isEmpty(serial) ? "UNAVAILABLE" : serial,
                firstPropOr(POYNT_OS_PROPS, Build.DISPLAY),
                Build.VERSION.RELEASE,
                abiList(),
                iotEnabled,
                iotSource
        );
    }

    @NonNull
    private static String firstProp(@NonNull List<String> keys) {
        for (String key : keys) {
            String value = systemProperty(key, "");
            if (!TextUtils.isEmpty(value) && !"unknown".equalsIgnoreCase(value)) {
                return value;
            }
        }
        return "";
    }

    @NonNull
    private static String firstPropOr(@NonNull List<String> keys, @Nullable String fallback) {
        String value = firstProp(keys);
        if (!TextUtils.isEmpty(value)) {
            return value;
        }
        return fallback == null ? "UNKNOWN" : fallback;
    }

    @NonNull
    private static String abiList() {
        if (Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0) {
            return TextUtils.join(",", Build.SUPPORTED_ABIS);
        }
        return "UNKNOWN";
    }

    @NonNull
    private static String safeBuildSerial() {
        try {
            String serial = Build.getSerial();
            if (!TextUtils.isEmpty(serial) && !"unknown".equalsIgnoreCase(serial)) {
                return serial;
            }
        } catch (SecurityException ignored) {
            // Companion is not a privileged phone-state app; fall back to properties.
        }
        return "";
    }

    private static boolean isTruthy(@NonNull String value) {
        String normalized = value.trim().toLowerCase();
        return "1".equals(normalized)
                || "true".equals(normalized)
                || "yes".equals(normalized)
                || "enabled".equals(normalized);
    }

    @NonNull
    public static String systemProperty(@NonNull String key, @NonNull String defaultValue) {
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method get = clazz.getMethod("get", String.class, String.class);
            Object value = get.invoke(null, key, defaultValue);
            return value == null ? defaultValue : String.valueOf(value);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    @NonNull
    private static String nullToEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }
}
