package co.poynt.iot.companion.shared.iot;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import co.poynt.iot.companion.shared.device.DeviceInspector;
import co.poynt.iot.companion.shared.device.DeviceSnapshot;
import co.poynt.iot.companion.shared.pcm.ProductionAppSnapshot;

/**
 * Phase 1 IoT status is inferred from device properties and the installed production APK.
 * Discover / token / MQTT execution stays behind the production foundation (Phase 2).
 */
public final class IotStatusReader {

    @NonNull
    public IotStatusSnapshot read(@NonNull DeviceSnapshot device, @NonNull ProductionAppSnapshot production) {
        Eligibility eligibility = eligibility(device, production);

        String discoveryUrl = DeviceInspector.systemProperty("persist.poynt.pcm.discovery", "");
        if (TextUtils.isEmpty(discoveryUrl)) {
            discoveryUrl = DeviceInspector.systemProperty("persist.poynt.discovery", "");
        }
        String pcmEndpoint = DeviceInspector.systemProperty("persist.poynt.srvc.url.pcm", "");
        if (TextUtils.isEmpty(pcmEndpoint)) {
            pcmEndpoint = DeviceInspector.systemProperty("persist.poynt.pcm.endpoint", "");
        }

        String discover;
        String discoverDetail;
        if (!TextUtils.isEmpty(discoveryUrl)) {
            discover = IotStatusSnapshot.PASS;
            discoverDetail = "Discovery URL configured on device";
        } else {
            discover = IotStatusSnapshot.UNKNOWN;
            discoverDetail = "No persist.poynt.pcm.discovery value (Discover API is Phase 2)";
        }

        return new IotStatusSnapshot(
                eligibility.status,
                eligibility.detail,
                discover,
                discoverDetail,
                IotStatusSnapshot.PENDING,
                IotStatusSnapshot.PENDING,
                IotStatusSnapshot.PENDING,
                "—",
                emptyToDash(discoveryUrl),
                emptyToDash(pcmEndpoint)
        );
    }

    @NonNull
    private static Eligibility eligibility(@NonNull DeviceSnapshot device, @NonNull ProductionAppSnapshot production) {
        if (!production.installed) {
            return new Eligibility(
                    IotStatusSnapshot.FAIL,
                    "Production package " + production.packageName + " is not installed"
            );
        }
        if (!production.enabled) {
            return new Eligibility(IotStatusSnapshot.FAIL, "Production Cloud Messaging package is disabled");
        }
        // Matches production IoTConstants.IOT_SUPPORTED_MODELS (P70) plus PCM presence.
        boolean p70 = "P70".equalsIgnoreCase(device.model) || "P70".equalsIgnoreCase(device.product);
        if (p70 && production.installed) {
            return new Eligibility(
                    IotStatusSnapshot.PASS,
                    "Model P70 is in production IOT_SUPPORTED_MODELS and PCM is installed"
            );
        }
        if (device.looksLikePoyntTerminal() && production.installed) {
            String iot = device.iotEnabledHint ? "IoT hint=YES" : "IoT hint=" + device.iotEnabledLabel();
            return new Eligibility(
                    IotStatusSnapshot.UNKNOWN,
                    "PCM installed; PST3 is not in IOT_SUPPORTED_MODELS (P70 only) unless iot_enabled_v2 is set (" + iot + ")"
            );
        }
        return new Eligibility(
                IotStatusSnapshot.UNKNOWN,
                "PCM installed, but this build does not look like PST3/P70. Confirm model mapping after foundation clone."
        );
    }

    @NonNull
    private static String emptyToDash(@NonNull String value) {
        return TextUtils.isEmpty(value) ? "—" : value;
    }

    private static final class Eligibility {
        final String status;
        final String detail;

        Eligibility(String status, String detail) {
            this.status = status;
            this.detail = detail;
        }
    }
}
