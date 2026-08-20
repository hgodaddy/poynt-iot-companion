package co.poynt.iot.companion.shared.pcm;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;

import androidx.annotation.NonNull;

import co.poynt.iot.companion.shared.BuildConfig;

/**
 * Inspects the production Cloud Messaging package that is already on the terminal.
 * Does not copy production code — it observes the real APK via PackageManager.
 */
public final class ProductionAppInspector {

    public static final int FLAG_PRIVILEGED = 1 << 30;

    @NonNull
    public ProductionAppSnapshot inspect(@NonNull Context context) {
        String packageName = BuildConfig.PRODUCTION_PACKAGE;
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_SERVICES);
            ApplicationInfo appInfo = info.applicationInfo;
            boolean privileged = appInfo != null && (appInfo.flags & FLAG_PRIVILEGED) != 0;
            boolean enabled = appInfo == null || appInfo.enabled;
            long versionCode;
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                versionCode = info.getLongVersionCode();
            } else {
                versionCode = info.versionCode;
            }
            ServiceMatch service = findPcmService(info);
            return new ProductionAppSnapshot(
                    packageName,
                    true,
                    enabled,
                    privileged,
                    info.versionName,
                    versionCode,
                    service.name,
                    service.exported
            );
        } catch (PackageManager.NameNotFoundException e) {
            return new ProductionAppSnapshot(
                    packageName,
                    false,
                    false,
                    false,
                    null,
                    0L,
                    BuildConfig.PRODUCTION_SERVICE,
                    false
            );
        }
    }

    @NonNull
    private static ServiceMatch findPcmService(@NonNull PackageInfo info) {
        if (info.services == null) {
            return new ServiceMatch(BuildConfig.PRODUCTION_SERVICE, false);
        }
        for (ServiceInfo service : info.services) {
            if (service.name != null && service.name.toLowerCase().contains("pcm")) {
                String component = service.packageName + "/" + service.name;
                return new ServiceMatch(component, service.exported);
            }
        }
        return new ServiceMatch(BuildConfig.PRODUCTION_SERVICE, false);
    }

    private static final class ServiceMatch {
        final String name;
        final boolean exported;

        ServiceMatch(String name, boolean exported) {
            this.name = name;
            this.exported = exported;
        }
    }
}
