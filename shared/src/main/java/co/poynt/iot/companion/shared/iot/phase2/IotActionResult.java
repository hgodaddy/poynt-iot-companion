package co.poynt.iot.companion.shared.iot.phase2;

import androidx.annotation.NonNull;

public final class IotActionResult {

    public final IotAction action;
    public final boolean pass;
    public final String status;
    public final String detail;

    public IotActionResult(@NonNull IotAction action, boolean pass, @NonNull String detail) {
        this.action = action;
        this.pass = pass;
        this.status = pass ? "PASS" : "FAIL";
        this.detail = detail;
    }

    @NonNull
    @Override
    public String toString() {
        return action + "=" + status + " — " + detail;
    }
}
