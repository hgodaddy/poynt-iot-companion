package co.poynt.iot.companion.shared;

import androidx.annotation.NonNull;

import co.poynt.iot.companion.shared.device.DeviceSnapshot;
import co.poynt.iot.companion.shared.foundation.FoundationBinder;
import co.poynt.iot.companion.shared.iot.IotStatusSnapshot;
import co.poynt.iot.companion.shared.pcm.ProductionAppSnapshot;

/**
 * Single dashboard model so UI, evidence log, and later JSON export share one snapshot.
 */
public final class DashboardSnapshot {

    public final DeviceSnapshot device;
    public final ProductionAppSnapshot production;
    public final IotStatusSnapshot iot;
    public final FoundationBinder.FoundationStatus foundation;
    public final String environment;
    public final String iotEndpoint;

    public DashboardSnapshot(
            @NonNull DeviceSnapshot device,
            @NonNull ProductionAppSnapshot production,
            @NonNull IotStatusSnapshot iot,
            @NonNull FoundationBinder.FoundationStatus foundation,
            @NonNull String environment,
            @NonNull String iotEndpoint) {
        this.device = device;
        this.production = production;
        this.iot = iot;
        this.foundation = foundation;
        this.environment = environment;
        this.iotEndpoint = iotEndpoint;
    }
}
