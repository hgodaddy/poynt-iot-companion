package co.poynt.cloudmessaging.iot.test.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import co.poynt.cloudmessaging.iot.test.CompanionApp;
import co.poynt.cloudmessaging.iot.test.databinding.ActivityDashboardBinding;
import co.poynt.iot.companion.shared.CompanionFacade;
import co.poynt.iot.companion.shared.DashboardSnapshot;
import co.poynt.iot.companion.shared.iot.IotStatusSnapshot;

public class DashboardActivity extends AppCompatActivity {

    private ActivityDashboardBinding binding;
    private CompanionFacade facade;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        facade = ((CompanionApp) getApplication()).facade();
        binding.btnRefresh.setOnClickListener(v -> render());
        render();
    }

    private void render() {
        DashboardSnapshot snapshot = facade.refresh(this);
        bindDevice(snapshot);
        bindIot(snapshot);
        binding.evidenceLog.setText(facade.logger().asText());
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
        binding.valueLastMessage.setText(iot.lastMessageAt);
    }

    private void setStatus(@NonNull TextView view, @NonNull String status) {
        view.setText(status);
        if (IotStatusSnapshot.PASS.equals(status) || "CONNECTED".equals(status)) {
            view.setTextColor(Color.parseColor("#3DDC97"));
        } else if (IotStatusSnapshot.FAIL.equals(status)) {
            view.setTextColor(Color.parseColor("#FF6B6B"));
        } else {
            view.setTextColor(Color.parseColor("#F4C95D"));
        }
    }

    private void tint(@NonNull TextView view, boolean ok) {
        view.setTextColor(Color.parseColor(ok ? "#3DDC97" : "#F4C95D"));
    }
}
