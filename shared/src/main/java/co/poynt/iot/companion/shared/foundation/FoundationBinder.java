package co.poynt.iot.companion.shared.foundation;

import androidx.annotation.NonNull;

import java.io.File;

import co.poynt.iot.companion.shared.BuildConfig;

/**
 * Tracks whether the production {@code poynt-cloudmessaging} source is present and bound.
 * Phase 1 observes the installed APK. Phase 2 binds in-process IoT classes from the clone.
 */
public final class FoundationBinder {

    public static final String FOUNDATION_RELATIVE_PATH = "foundation/poynt-cloudmessaging";
    public static final String DEFAULT_CLONE_URL = "https://github.com/gdcorp-commerce/poynt-cloudmessaging.git";

    @NonNull
    public FoundationStatus status() {
        boolean compileBound = BuildConfig.FOUNDATION_BOUND;
        return new FoundationStatus(
                compileBound || co.poynt.iot.companion.shared.BuildConfig.IOT_PROTOCOL_BOUND,
                compileBound
                        ? "Production IoT modules are compiled into this APK"
                        : "Phase 2 protocol bound — Discover/MQTT5/token flow matches production aws/ stack"
        );
    }

    /**
     * Host-side helper used by {@code scripts/map-foundation.sh}, not by the APK.
     */
    public static boolean sourceTreePresent(@NonNull File repoRoot) {
        File clone = new File(repoRoot, FOUNDATION_RELATIVE_PATH);
        return clone.isDirectory() && (new File(clone, ".git").exists()
                || new File(clone, "settings.gradle").exists()
                || new File(clone, "settings.gradle.kts").exists()
                || new File(clone, "build.gradle").exists()
                || new File(clone, "pom.xml").exists());
    }

    public static final class FoundationStatus {
        public final boolean bound;
        public final String detail;

        public FoundationStatus(boolean bound, @NonNull String detail) {
            this.bound = bound;
            this.detail = detail;
        }

        @NonNull
        public String label() {
            return bound ? "BOUND" : "NOT BOUND";
        }
    }
}
