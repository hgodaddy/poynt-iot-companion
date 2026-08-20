package co.poynt.iot.companion.shared.iot.phase2;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import co.poynt.iot.companion.shared.iot.IotStatusSnapshot;

public final class IotRuntimeState {

    public String eligibility = IotStatusSnapshot.UNKNOWN;
    public String eligibilityDetail = "";
    public String discover = IotStatusSnapshot.UNKNOWN;
    public String discoverDetail = "";
    public String gdToken = IotStatusSnapshot.UNKNOWN;
    public String gdTokenDetail = "";
    public String mqtt = IotStatusSnapshot.UNKNOWN;
    public String subscription = IotStatusSnapshot.UNKNOWN;
    public String publish = IotStatusSnapshot.UNKNOWN;
    public String receive = IotStatusSnapshot.UNKNOWN;
    public String reconnect = IotStatusSnapshot.UNKNOWN;
    public String lastMessageAt = "—";
    public String lastMessagePreview = "";
    public String lastCorrelationId = "";
    public boolean lastReceiveValidated;
    public boolean iotEnabled;
    public String mothershipUrl = "—";
    public String iotEndpoint = "—";
    public String authorizer = ProductionIotConstants.DEFAULT_CUSTOM_AUTHORIZER;
    public String jobsTopic = "";
    public String clientId = "";
    public String discoveryUrl = "—";
    public String pcmEndpoint = "—";
    public String tokenFingerprint = "none";
    public String overall = IotStatusSnapshot.UNKNOWN;

    public void onMessage(@NonNull String topic, @NonNull String payload) {
        lastMessageAt = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        lastMessagePreview = payload.length() > 80 ? payload.substring(0, 80) + "…" : payload;
        if (lastCorrelationId.length() > 0 && payload.contains(lastCorrelationId)) {
            lastReceiveValidated = true;
            receive = IotStatusSnapshot.PASS;
        }
    }

    public void computeOverall() {
        String[] gates = {eligibility, discover, gdToken, mqtt, subscription, publish, receive, reconnect};
        boolean anyFail = false;
        boolean anyUnknown = false;
        for (String gate : gates) {
            if (IotStatusSnapshot.FAIL.equals(gate)) {
                anyFail = true;
            }
            if (IotStatusSnapshot.UNKNOWN.equals(gate) || IotStatusSnapshot.PENDING.equals(gate)
                    || gate == null || gate.isEmpty()) {
                anyUnknown = true;
            }
        }
        if (anyFail) {
            overall = IotStatusSnapshot.FAIL;
        } else if (anyUnknown) {
            overall = IotStatusSnapshot.UNKNOWN;
        } else {
            overall = IotStatusSnapshot.PASS;
        }
    }
}
