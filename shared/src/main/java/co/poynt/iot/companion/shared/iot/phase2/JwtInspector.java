package co.poynt.iot.companion.shared.iot.phase2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Base64;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Inspects a GD JWT without logging the token value.
 */
public final class JwtInspector {

    public final boolean present;
    public final boolean expired;
    public final String fingerprint;
    public final String deviceId;
    public final String summary;
    public final long expiresAtEpochMs;

    private JwtInspector(boolean present, boolean expired, String fingerprint, String deviceId,
                         String summary, long expiresAtEpochMs) {
        this.present = present;
        this.expired = expired;
        this.fingerprint = fingerprint;
        this.deviceId = deviceId;
        this.summary = summary;
        this.expiresAtEpochMs = expiresAtEpochMs;
    }

    @NonNull
    public static JwtInspector inspect(@Nullable String token) {
        if (token == null || token.trim().isEmpty()) {
            return new JwtInspector(false, true, "none", "", "MISSING", 0L);
        }
        String jwt = token.trim();
        String fingerprint = "sha256:" + sha256Prefix(jwt);
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) {
                return new JwtInspector(true, true, fingerprint, "", "PRESENT but not a JWT", 0L);
            }
            String payload = pad(parts[1]);
            byte[] decoded = Base64.decode(payload, Base64.URL_SAFE | Base64.NO_WRAP);
            JSONObject claims = new JSONObject(new String(decoded, StandardCharsets.UTF_8));
            long expSec = claims.optLong("exp", 0L);
            long expMs = expSec > 0 ? expSec * 1000L : 0L;
            boolean expired = expMs > 0 && System.currentTimeMillis() >= expMs - (5 * 60 * 1000L);
            String deviceId = firstClaim(claims, "deviceId", "device_id", "did", "sub");
            String summary = expired ? "EXPIRED" : "PRESENT";
            if (expMs > 0) {
                long minutes = Math.max(0, (expMs - System.currentTimeMillis()) / 60000L);
                summary = summary + " fingerprint=" + fingerprint + " remaining=" + minutes + "m";
            } else {
                summary = summary + " fingerprint=" + fingerprint;
            }
            return new JwtInspector(true, expired, fingerprint, deviceId == null ? "" : deviceId, summary, expMs);
        } catch (Exception e) {
            return new JwtInspector(true, true, fingerprint, "", "PRESENT parse-error fingerprint=" + fingerprint, 0L);
        }
    }

    @Nullable
    private static String firstClaim(@NonNull JSONObject claims, String... keys) {
        for (String key : keys) {
            String value = claims.optString(key, "");
            if (!value.isEmpty() && !"null".equals(value)) {
                return value;
            }
        }
        return null;
    }

    @NonNull
    private static String pad(@NonNull String b64) {
        int mod = b64.length() % 4;
        if (mod == 0) {
            return b64;
        }
        StringBuilder builder = new StringBuilder(b64);
        for (int i = 0; i < 4 - mod; i++) {
            builder.append('=');
        }
        return builder.toString();
    }

    @NonNull
    private static String sha256Prefix(@NonNull String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 6 && i < hash.length; i++) {
                hex.append(String.format("%02x", hash[i]));
            }
            return hex.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
