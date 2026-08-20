package co.poynt.iot.companion.shared.pcm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Runtime view of the installed production Cloud Messaging APK.
 * This is how the companion coexists with {@code co.poynt.cloudmessaging} on the same terminal.
 */
public final class ProductionAppSnapshot {

    public final String packageName;
    public final boolean installed;
    public final boolean enabled;
    public final boolean privileged;
    public final String versionName;
    public final long versionCode;
    public final String serviceName;
    public final boolean serviceExported;

    public ProductionAppSnapshot(
            @NonNull String packageName,
            boolean installed,
            boolean enabled,
            boolean privileged,
            @Nullable String versionName,
            long versionCode,
            @NonNull String serviceName,
            boolean serviceExported) {
        this.packageName = packageName;
        this.installed = installed;
        this.enabled = enabled;
        this.privileged = privileged;
        this.versionName = versionName == null ? "—" : versionName;
        this.versionCode = versionCode;
        this.serviceName = serviceName;
        this.serviceExported = serviceExported;
    }

    @NonNull
    public String installedLabel() {
        if (!installed) {
            return "NOT INSTALLED";
        }
        return enabled ? "INSTALLED" : "DISABLED";
    }
}
