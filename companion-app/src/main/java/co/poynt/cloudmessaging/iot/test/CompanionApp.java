package co.poynt.cloudmessaging.iot.test;

import android.app.Application;

import co.poynt.iot.companion.shared.CompanionFacade;
import co.poynt.iot.companion.shared.logging.EvidenceLogger;

public class CompanionApp extends Application {

    private CompanionFacade facade;

    @Override
    public void onCreate() {
        super.onCreate();
        EvidenceLogger logger = new EvidenceLogger();
        logger.info("Companion Test App v0.4 Phase 4 started");
        facade = new CompanionFacade(this, logger);
    }

    public CompanionFacade facade() {
        return facade;
    }
}
