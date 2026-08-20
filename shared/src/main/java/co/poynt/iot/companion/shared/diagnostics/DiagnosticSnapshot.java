package co.poynt.iot.companion.shared.diagnostics;

import androidx.annotation.NonNull;

import co.poynt.iot.companion.shared.iot.phase2.IotRuntimeState;

public final class DiagnosticSnapshot {

    public final String networkSummary;
    public final boolean wifiEnabled;
    public final boolean httpsReachable;
    public final long latencyMs;
    public final String transport;
    public final String mqttState;
    public final int mqttAttempts;
    public final String mqttLastError;
    public final String tokenState;
    public final String tokenFingerprint;
    public final String lastErrorCode;
    public final String lastErrorDetail;
    public final String evidencePath;

    public DiagnosticSnapshot(
            @NonNull String networkSummary,
            boolean wifiEnabled,
            boolean httpsReachable,
            long latencyMs,
            @NonNull String transport,
            @NonNull String mqttState,
            int mqttAttempts,
            @NonNull String mqttLastError,
            @NonNull String tokenState,
            @NonNull String tokenFingerprint,
            @NonNull String lastErrorCode,
            @NonNull String lastErrorDetail,
            @NonNull String evidencePath) {
        this.networkSummary = networkSummary;
        this.wifiEnabled = wifiEnabled;
        this.httpsReachable = httpsReachable;
        this.latencyMs = latencyMs;
        this.transport = transport;
        this.mqttState = mqttState;
        this.mqttAttempts = mqttAttempts;
        this.mqttLastError = mqttLastError;
        this.tokenState = tokenState;
        this.tokenFingerprint = tokenFingerprint;
        this.lastErrorCode = lastErrorCode;
        this.lastErrorDetail = lastErrorDetail;
        this.evidencePath = evidencePath;
    }

    @NonNull
    public static DiagnosticSnapshot from(
            @NonNull NetworkInspector.NetworkSnapshot network,
            @NonNull IotRuntimeState runtime,
            @NonNull String evidencePath) {
        return new DiagnosticSnapshot(
                network.summary(),
                network.wifiEnabled,
                network.httpsReachable,
                network.latencyMs,
                network.transport,
                runtime.mqttState,
                runtime.mqttAttempts,
                runtime.mqttLastError == null ? "—" : runtime.mqttLastError,
                runtime.tokenState == null ? "UNKNOWN" : runtime.tokenState,
                runtime.tokenFingerprint,
                runtime.lastErrorCode == null ? ErrorCode.NONE.code : runtime.lastErrorCode,
                runtime.lastErrorDetail == null ? "—" : runtime.lastErrorDetail,
                evidencePath
        );
    }
}
