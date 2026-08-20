package co.poynt.iot.companion.shared;

import android.content.Context;

import androidx.annotation.NonNull;

import co.poynt.iot.companion.shared.config.EnvironmentConfig;
import co.poynt.iot.companion.shared.device.DeviceInspector;
import co.poynt.iot.companion.shared.device.DeviceSnapshot;
import co.poynt.iot.companion.shared.foundation.FoundationBinder;
import co.poynt.iot.companion.shared.iot.IotStatusSnapshot;
import co.poynt.iot.companion.shared.iot.phase2.IotAction;
import co.poynt.iot.companion.shared.iot.phase2.IotController;
import co.poynt.iot.companion.shared.iot.phase2.IotRuntimeState;
import co.poynt.iot.companion.shared.logging.EvidenceLogger;
import co.poynt.iot.companion.shared.pcm.ProductionAppInspector;
import co.poynt.iot.companion.shared.pcm.ProductionAppSnapshot;

public final class CompanionFacade {

    private final DeviceInspector deviceInspector = new DeviceInspector();
    private final ProductionAppInspector productionInspector = new ProductionAppInspector();
    private final FoundationBinder foundationBinder = new FoundationBinder();
    private final EnvironmentConfig environmentConfig;
    private final EvidenceLogger evidenceLogger;
    private final IotController iotController;

    public CompanionFacade(@NonNull Context context, @NonNull EvidenceLogger evidenceLogger) {
        Context app = context.getApplicationContext();
        this.environmentConfig = new EnvironmentConfig(app);
        this.evidenceLogger = evidenceLogger;
        this.iotController = new IotController(app, evidenceLogger);
    }

    @NonNull
    public EvidenceLogger logger() {
        return evidenceLogger;
    }

    @NonNull
    public IotController iot() {
        return iotController;
    }

    @NonNull
    public DashboardSnapshot refresh(@NonNull Context context) {
        DeviceSnapshot device = deviceInspector.inspect();
        ProductionAppSnapshot production = productionInspector.inspect(context);
        IotRuntimeState runtime = iotController.state();
        FoundationBinder.FoundationStatus foundation = foundationBinder.status();
        IotStatusSnapshot iot = IotStatusSnapshot.fromRuntime(runtime);
        return new DashboardSnapshot(
                device,
                production,
                iot,
                foundation,
                environmentConfig.environment(),
                "—".equals(runtime.iotEndpoint) ? environmentConfig.iotEndpoint() : runtime.iotEndpoint,
                iotController.diagnostics()
        );
    }
}
