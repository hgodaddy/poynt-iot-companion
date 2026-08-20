package co.poynt.iot.companion.shared.diagnostics;

import androidx.annotation.NonNull;

/**
 * One negative-path scenario. PASS means the failure was detected correctly.
 */
public final class NegativeScenarioResult {

    public final String scenario;
    public final String status;
    public final String errorCode;
    public final String detail;

    public NegativeScenarioResult(@NonNull String scenario, @NonNull String status,
                                  @NonNull String errorCode, @NonNull String detail) {
        this.scenario = scenario;
        this.status = status;
        this.errorCode = errorCode;
        this.detail = detail;
    }

    @NonNull
    public static NegativeScenarioResult pass(@NonNull String scenario, @NonNull ErrorCode code, @NonNull String detail) {
        return new NegativeScenarioResult(scenario, "PASS", code.code, detail);
    }

    @NonNull
    public static NegativeScenarioResult fail(@NonNull String scenario, @NonNull ErrorCode code, @NonNull String detail) {
        return new NegativeScenarioResult(scenario, "FAIL", code.code, detail);
    }

    @NonNull
    public static NegativeScenarioResult skipped(@NonNull String scenario, @NonNull ErrorCode code, @NonNull String detail) {
        return new NegativeScenarioResult(scenario, "SKIPPED", code.code, detail);
    }

    public boolean isFail() {
        return "FAIL".equals(status);
    }

    @NonNull
    @Override
    public String toString() {
        return scenario + "=" + status + " [" + errorCode + "] " + detail;
    }
}
