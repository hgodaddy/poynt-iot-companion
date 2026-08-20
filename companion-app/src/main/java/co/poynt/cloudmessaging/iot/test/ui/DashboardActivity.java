package co.poynt.cloudmessaging.iot.test.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import co.poynt.cloudmessaging.iot.test.CompanionApp;
import co.poynt.cloudmessaging.iot.test.databinding.ActivityDashboardBinding;
import co.poynt.iot.companion.shared.CompanionFacade;
import co.poynt.iot.companion.shared.DashboardSnapshot;
import co.poynt.iot.companion.shared.diagnostics.DiagnosticSnapshot;
import co.poynt.iot.companion.shared.iot.IotStatusSnapshot;
import co.poynt.iot.companion.shared.iot.phase2.IotAction;
import co.poynt.iot.companion.shared.iot.phase2.IotActionResult;
import co.poynt.iot.companion.shared.iot.phase2.IotController;

public class DashboardActivity extends AppCompatActivity implements IotController.Listener {

    private ActivityDashboardBinding binding;
    private CompanionFacade facade;
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        facade = ((CompanionApp) getApplication()).facade();
        facade.iot().setListener(this);

        binding.btnRefresh.setOnClickListener(v -> render("manual refresh"));
        binding.btnCheckEligibility.setOnClickListener(v -> run(IotAction.ELIGIBILITY));
        binding.btnRunDiscover.setOnClickListener(v -> run(IotAction.DISCOVER));
        binding.btnRefreshToken.setOnClickListener(v -> run(IotAction.TOKEN));
        binding.btnConnectMqtt.setOnClickListener(v -> run(IotAction.MQTT_CONNECT));
        binding.btnDisconnectMqtt.setOnClickListener(v -> run(IotAction.DISCONNECT));
        binding.btnReconnect.setOnClickListener(v -> run(IotAction.RECONNECT));
        binding.btnSubscribe.setOnClickListener(v -> run(IotAction.SUBSCRIBE));
        binding.btnPublish.setOnClickListener(v -> run(IotAction.PUBLISH));
        binding.btnFullFlow.setOnClickListener(v -> run(IotAction.FULL_FLOW));
        binding.btnDiagnostics.setOnClickListener(v -> run(IotAction.DIAGNOSTICS));
        binding.btnCollectLogs.setOnClickListener(v -> run(IotAction.EXPORT));

        render("startup");
    }

    @Override
    protected void onDestroy() {
        facade.iot().setListener(null);
        super.onDestroy();
    }

    private void run(@NonNull IotAction action) {
        if (facade.iot().isBusy()) {
            Toast.makeText(this, "An IoT action is already running", Toast.LENGTH_SHORT).show();
            return;
        }
        facade.logger().info("UI requested " + action);
        setButtonsEnabled(false);
        facade.iot().execute(action);
        render(action.name());
    }

    private void render(@NonNull String reason) {
        DashboardSnapshot snapshot = facade.refresh(this);
        bindDevice(snapshot);
        bindIot(snapshot);
        bindDiagnostics(snapshot.diagnostics);
        binding.evidenceLog.setText(facade.logger().asText());
        setButtonsEnabled(!facade.iot().isBusy());
    }

    @Override
    public void onUpdated() {
        main.post(() -> render("iot-update"));
    }

    @Override
    public void onComplete(@NonNull IotActionResult result) {
        main.post(() -> {
            Toast.makeText(this, result.toString(), Toast.LENGTH_LONG).show();
            render("iot-complete");
        });
    }

    private void bindDevice(@NonNull DashboardSnapshot snapshot) {
        binding.valueModel.setText(snapshot.device.displayModel());
        binding.valueSerial.setText(snapshot.device.serial);
        binding.valuePoyntOs.setText(snapshot.device.poyntOsVersion);
        binding.valueIotEnabled.setText(snapshot.device.iotEnabledLabel());
        tint(binding.valueIotEnabled, snapshot.device.iotEnabledHint || "PASS".equals(snapshot.iot.eligibility));
        binding.valueProduction.setText(snapshot.production.installedLabel()
                + "  v" + snapshot.production.versionName
                + "  priv=" + (snapshot.production.privileged ? "YES" : "NO"));
        tint(binding.valueProduction, snapshot.production.installed && snapshot.production.enabled);
        binding.valueFoundation.setText(snapshot.foundation.label());
        tint(binding.valueFoundation, snapshot.foundation.bound);
        binding.valueEnvironment.setText(snapshot.environment + "  /  " + snapshot.iotEndpoint);
        binding.valueDiscovery.setText(snapshot.iot.discoveryUrl);
        binding.valuePcmEndpoint.setText(snapshot.iot.pcmEndpoint);
        binding.valueMothership.setText(snapshot.iot.mothershipUrl);
    }

    private void bindIot(@NonNull DashboardSnapshot snapshot) {
        IotStatusSnapshot iot = snapshot.iot;
        setStatus(binding.valueEligibility, iot.eligibility);
        setStatus(binding.valueDiscover, iot.discover);
        setStatus(binding.valueToken, iot.gdToken);
        setStatus(binding.valueMqtt, iot.mqtt);
        setStatus(binding.valueSubscription, iot.subscription);
        setStatus(binding.valuePublish, iot.publish);
        setStatus(binding.valueReceive, iot.receive);
        setStatus(binding.valueReconnect, iot.reconnect);
        setStatus(binding.valueOverall, iot.overall);
        String last = iot.lastMessageAt;
        if (iot.tokenDetail != null && !iot.tokenDetail.isEmpty()) {
            binding.valueToken.setText(iot.gdToken);
        }
        binding.valueLastMessage.setText(last);
    }

    private void bindDiagnostics(@NonNull DiagnosticSnapshot diagnostics) {
        binding.valueNetwork.setText(diagnostics.networkSummary);
        tint(binding.valueNetwork, diagnostics.httpsReachable || diagnostics.wifiEnabled);

        binding.valueMqttState.setText(diagnostics.mqttState + "  attempts=" + diagnostics.mqttAttempts);
        colorStatus(binding.valueMqttState, diagnostics.mqttState);

        binding.valueTokenState.setText(diagnostics.tokenState + "  fp=" + diagnostics.tokenFingerprint);
        String tokenGate = "PRESENT".equals(diagnostics.tokenState)
                ? IotStatusSnapshot.PASS
                : ("UNKNOWN".equals(diagnostics.tokenState) ? IotStatusSnapshot.UNKNOWN : IotStatusSnapshot.FAIL);
        colorStatus(binding.valueTokenState, tokenGate);

        binding.valueLastError.setText(diagnostics.lastErrorCode + "  " + diagnostics.lastErrorDetail);
        boolean noError = "IOT-000".equals(diagnostics.lastErrorCode);
        binding.valueLastError.setTextColor(Color.parseColor(noError ? "#A0A0A0" : "#FF6B6B"));
    }

    private void setButtonsEnabled(boolean enabled) {
        binding.btnRefresh.setEnabled(true);
        binding.btnCheckEligibility.setEnabled(enabled);
        binding.btnRunDiscover.setEnabled(enabled);
        binding.btnRefreshToken.setEnabled(enabled);
        binding.btnConnectMqtt.setEnabled(enabled);
        binding.btnDisconnectMqtt.setEnabled(enabled);
        binding.btnReconnect.setEnabled(enabled);
        binding.btnSubscribe.setEnabled(enabled);
        binding.btnPublish.setEnabled(enabled);
        binding.btnFullFlow.setEnabled(enabled);
        binding.btnDiagnostics.setEnabled(enabled);
        binding.btnCollectLogs.setEnabled(enabled);
    }

    private void setStatus(@NonNull TextView view, @NonNull String status) {
        view.setText(status);
        colorStatus(view, status);
    }

    private void colorStatus(@NonNull TextView view, @NonNull String status) {
        if (IotStatusSnapshot.PASS.equals(status) || "CONNECTED".equals(status) || "PRESENT".equals(status)) {
            view.setTextColor(Color.parseColor("#3DDC97"));
        } else if (IotStatusSnapshot.FAIL.equals(status)
                || "EXPIRED".equals(status)
                || "MISSING".equals(status)
                || "INVALID".equals(status)) {
            view.setTextColor(Color.parseColor("#FF6B6B"));
        } else {
            view.setTextColor(Color.parseColor("#F4C95D"));
        }
    }

    private void tint(@NonNull TextView view, boolean ok) {
        view.setTextColor(Color.parseColor(ok ? "#3DDC97" : "#F4C95D"));
    }
}
