package co.poynt.iot.companion.shared.device;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Live device identity used by the Phase 1 dashboard.
 * Values come from Android / Poynt system properties — not from production secrets.
 */
public final class DeviceSnapshot {

    public final String model;
    public final String manufacturer;
    public final String product;
    public final String serial;
    public final String poyntOsVersion;
    public final String androidRelease;
    public final String abi;
    public final boolean iotEnabledHint;
    public final String iotEnabledSource;

    public DeviceSnapshot(
            @NonNull String model,
            @NonNull String manufacturer,
            @NonNull String product,
            @NonNull String serial,
            @NonNull String poyntOsVersion,
            @NonNull String androidRelease,
            @NonNull String abi,
            boolean iotEnabledHint,
            @NonNull String iotEnabledSource) {
        this.model = model;
        this.manufacturer = manufacturer;
        this.product = product;
        this.serial = serial;
        this.poyntOsVersion = poyntOsVersion;
        this.androidRelease = androidRelease;
        this.abi = abi;
        this.iotEnabledHint = iotEnabledHint;
        this.iotEnabledSource = iotEnabledSource;
    }

    @NonNull
    public String displayModel() {
        if (!model.isEmpty()) {
            return model;
        }
        return product.isEmpty() ? "UNKNOWN" : product;
    }

    @NonNull
    public String iotEnabledLabel() {
        if (iotEnabledHint) {
            return "YES";
        }
        if ("unset".equals(iotEnabledSource)) {
            return "UNKNOWN";
        }
        return "NO";
    }

    public boolean looksLikePoyntTerminal() {
        String haystack = (model + " " + product + " " + manufacturer).toUpperCase();
        return haystack.contains("PST3")
                || haystack.contains("P70")
                || haystack.contains("POYNT")
                || haystack.contains("ST3");
    }

    @Nullable
    public String toShortLine() {
        return displayModel() + "  serial=" + serial;
    }
}
