package co.poynt.iot.companion.shared;

import android.content.Context;

import androidx.annotation.NonNull;

import co.poynt.iot.companion.shared.config.EnvironmentConfig;
import co.poynt.iot.companion.shared.device.DeviceInspector;
import co.poynt.iot.companion.shared.device.DeviceSnapshot;
import co.poynt.iot.companion.shared.foundation.FoundationBinder;
import co.poynt.iot.companion.shared.iot.IotStatusReader;
import co.poynt.iot.companion.shared.iot.IotStatusSnapshot;
import co.poynt.iot.companion.shared.logging.EvidenceLogger;
import co.poynt.iot.companion.shared.pcm.ProductionAppInspector;
import co.poynt.iot.companion.shared.pcm.ProductionAppSnapshot;

public final class CompanionFacade {

    private final DeviceInspector deviceInspector = new DeviceInspector();
    private final ProductionAppInspector productionInspector = new ProductionAppInspector();
    private final FoundationBinder foundationBinder = new FoundationBinder();
    private final IotStatusReader iotStatusReader = new IotStatusReader();
    private final EnvironmentConfig environmentConfig;
    private final EvidenceLogger evidenceLogger;

    public CompanionFacade(@NonNull Context context, @NonNull EvidenceLogger evidenceLogger) {
        Context app = context.getApplicationContext();
        this.environmentConfig = new EnvironmentConfig(app);
        this.evidenceLogger = evidenceLogger;
    }

    @NonNull
    public EvidenceLogger logger() {
        return evidenceLogger;
    }

    @NonNull
    public DashboardSnapshot refresh(@NonNull Context context) {
        DeviceSnapshot device = deviceInspector.inspect();
        ProductionAppSnapshot production = productionInspector.inspect(context);
        FoundationBinder.FoundationStatus foundation = foundationBinder.status();
        IotStatusSnapshot iot = iotStatusReader.read(device, production);
        return new DashboardSnapshot(
                device,
                production,
                iot,
                foundation,
                environmentConfig.environment(),
                environmentConfig.iotEndpoint()
        );
    }
}
