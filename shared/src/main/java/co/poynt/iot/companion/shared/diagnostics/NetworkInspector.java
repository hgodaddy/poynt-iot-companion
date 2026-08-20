package co.poynt.iot.companion.shared.diagnostics;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Live network / Wi-Fi / internet diagnostics for the QA dashboard.
 */
public final class NetworkInspector {

    private final Context context;
    private final OkHttpClient probeClient = new OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .callTimeout(4, TimeUnit.SECONDS)
            .followRedirects(false)
            .build();

    public NetworkInspector(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    public NetworkSnapshot snapshot() {
        return snapshot(true);
    }

    @NonNull
    public NetworkSnapshot snapshot(boolean probeHttps) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        boolean wifiEnabled = wifi != null && wifi.isWifiEnabled();
        boolean connected = false;
        boolean validated = false;
        String transport = "NONE";
        if (cm != null) {
            if (Build.VERSION.SDK_INT >= 23) {
                Network active = cm.getActiveNetwork();
                NetworkCapabilities caps = active != null ? cm.getNetworkCapabilities(active) : null;
                if (caps != null) {
                    connected = true;
                    validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        transport = "WIFI";
                    } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                        transport = "ETHERNET";
                    } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        transport = "CELLULAR";
                    } else {
                        transport = "OTHER";
                    }
                }
            } else {
                NetworkInfo info = cm.getActiveNetworkInfo();
                connected = info != null && info.isConnected();
                validated = connected;
                transport = info != null && info.getTypeName() != null ? info.getTypeName() : "UNKNOWN";
            }
        }
        boolean httpsOk = validated;
        long latency = 0L;
        String detail = "probe-skipped";
        if (probeHttps) {
            ProbeResult https = probeHttps("https://fouroneone.poynt.net/discovery/services");
            httpsOk = https.ok;
            latency = https.latencyMs;
            detail = https.detail;
        }
        return new NetworkSnapshot(connected, wifiEnabled, validated, transport, httpsOk, latency, detail);
    }

    @NonNull
    public ProbeResult probeHttps(@NonNull String url) {
        long start = System.currentTimeMillis();
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = probeClient.newCall(request).execute()) {
            long latency = System.currentTimeMillis() - start;
            return new ProbeResult(true, latency, "HTTP " + response.code());
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            return new ProbeResult(false, latency, e.getMessage() == null ? "probe failed" : e.getMessage());
        }
    }

    @NonNull
    public ProbeResult probeUnreachable() {
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("192.0.2.1", 443), 2500);
            return new ProbeResult(true, System.currentTimeMillis() - start, "unexpected connect");
        } catch (IOException e) {
            return new ProbeResult(false, System.currentTimeMillis() - start,
                    e.getClass().getSimpleName());
        }
    }

    @NonNull
    public WifiToggleResult setWifiEnabled(boolean enabled) {
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            try {
                @SuppressWarnings("deprecation")
                boolean ok = wifi.setWifiEnabled(enabled);
                if (ok) {
                    return WifiToggleResult.applied("WifiManager.setWifiEnabled(" + enabled + ")");
                }
            } catch (SecurityException e) {
                // fall through to svc
            }
        }
        String arg = enabled ? "enable" : "disable";
        if (runShell(new String[]{"svc", "wifi", arg}) || runShell(new String[]{"su", "-c", "svc wifi " + arg})) {
            return WifiToggleResult.applied("svc wifi " + arg);
        }
        return WifiToggleResult.denied("Use: adb shell svc wifi " + arg);
    }

    private boolean runShell(@NonNull String[] command) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(command);
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    public static final class NetworkSnapshot {
        public final boolean connected;
        public final boolean wifiEnabled;
        public final boolean validatedInternet;
        public final String transport;
        public final boolean httpsReachable;
        public final long latencyMs;
        public final String probeDetail;

        public NetworkSnapshot(boolean connected, boolean wifiEnabled, boolean validatedInternet,
                               String transport, boolean httpsReachable, long latencyMs, String probeDetail) {
            this.connected = connected;
            this.wifiEnabled = wifiEnabled;
            this.validatedInternet = validatedInternet;
            this.transport = transport;
            this.httpsReachable = httpsReachable;
            this.latencyMs = latencyMs;
            this.probeDetail = probeDetail;
        }

        @NonNull
        public String summary() {
            return transport
                    + " wifi=" + (wifiEnabled ? "ON" : "OFF")
                    + " connected=" + connected
                    + " internet=" + (httpsReachable ? "YES" : "NO")
                    + " rtt=" + latencyMs + "ms";
        }
    }

    public static final class ProbeResult {
        public final boolean ok;
        public final long latencyMs;
        public final String detail;

        public ProbeResult(boolean ok, long latencyMs, String detail) {
            this.ok = ok;
            this.latencyMs = latencyMs;
            this.detail = detail;
        }
    }

    public static final class WifiToggleResult {
        public final boolean applied;
        public final String detail;

        private WifiToggleResult(boolean applied, String detail) {
            this.applied = applied;
            this.detail = detail;
        }

        static WifiToggleResult applied(String detail) {
            return new WifiToggleResult(true, detail);
        }

        static WifiToggleResult denied(String detail) {
            return new WifiToggleResult(false, detail);
        }
    }
}
