package com.poynt.phmp.iot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Drop-in for PHMP {@code ReleaseGateEvaluator}. Add {@link #IOT_CRITICAL} names to the
 * Phase 1 {@code CRITICAL} set (or a dedicated IoT set) so a skipped/failed companion
 * check blocks nightly ship.
 *
 * <p>Suggested {@code PcmHealthEngine} wiring after {@code CloudMessagingValidator}:
 * {@code runStep(summary, new IotCompanionValidator(serial, 360)::validate);}
 * then {@code new IotReleaseGateEvaluator().evaluate(results)}.
 *
 * <p>Map {@link Decision} onto PHMP {@code ValidationResult}. Check name must stay
 * {@code IoT Release Gate Decision}.
 */
public final class IotReleaseGateEvaluator {

    public static final String CHECK_NAME = "IoT Release Gate Decision";

    public static final Set<String> IOT_CRITICAL = Set.of(
            "Production Cloud Messaging APK",
            "Companion Test App",
            "Device / PoyntOS",
            "IoT Companion Gate"
    );

    public Decision evaluate(List<NamedResult> prior) {
        List<String> details = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        List<String> skippedCritical = new ArrayList<>();
        long criticalFails = 0;
        for (NamedResult result : prior) {
            if (!IOT_CRITICAL.contains(result.name)) {
                continue;
            }
            if (result.skipped) {
                skippedCritical.add(result.name);
            } else if (!result.passed) {
                criticalFails++;
                failures.add(result.name);
            }
        }
        details.add("IoT critical failures=" + criticalFails);
        details.add("IoT critical skipped=" + skippedCritical.size());
        if (criticalFails > 0) {
            return Decision.fail(CHECK_NAME,
                    "Build is NOT IoT release-ready",
                    failures);
        }
        if (!skippedCritical.isEmpty()) {
            details.add("Unevaluated: " + String.join(", ", skippedCritical));
            return Decision.skip(CHECK_NAME,
                    "Not certifiable: " + skippedCritical.size() + " IoT critical check(s) unevaluated",
                    details);
        }
        return Decision.pass(CHECK_NAME,
                "PASS — nightly IoT build is release-ready",
                details,
                Map.of("releaseReady", true, "criticalFailures", 0L));
    }

    public static final class NamedResult {
        public final String name;
        public final boolean passed;
        public final boolean skipped;

        public NamedResult(String name, boolean passed, boolean skipped) {
            this.name = name;
            this.passed = passed;
            this.skipped = skipped;
        }
    }

    public static final class Decision {
        public final String name;
        public final boolean passed;
        public final boolean skipped;
        public final String message;
        public final List<String> details;
        public final Map<String, Object> metrics;

        private Decision(String name, boolean passed, boolean skipped, String message,
                         List<String> details, Map<String, Object> metrics) {
            this.name = name;
            this.passed = passed;
            this.skipped = skipped;
            this.message = message;
            this.details = details;
            this.metrics = metrics;
        }

        public static Decision pass(String name, String message, List<String> details, Map<String, Object> metrics) {
            return new Decision(name, true, false, message, details, metrics);
        }

        public static Decision fail(String name, String message, List<String> details) {
            return new Decision(name, false, false, message, details, Map.of());
        }

        public static Decision skip(String name, String message, List<String> details) {
            return new Decision(name, true, true, message, details, Map.of("skipped", true));
        }
    }
}
