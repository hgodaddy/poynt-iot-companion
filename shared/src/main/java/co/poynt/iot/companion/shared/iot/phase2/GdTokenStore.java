package co.poynt.iot.companion.shared.iot.phase2;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import co.poynt.iot.companion.shared.config.EnvironmentConfig;
import co.poynt.iot.companion.shared.logging.EvidenceLogger;

/**
 * Obtains a GD JWT the same way QA already recovers it: companion prefs / injected broadcast /
 * PCM logcat connect URL. Does not call Poynt AIDL (that SDK is not on this classpath).
 * Never writes the raw token to evidence.
 */
public final class GdTokenStore {

    public static final String KEY_GD_TOKEN = "gd_token";
    private static final Pattern TOKEN_QUERY = Pattern.compile("[?&]token=([A-Za-z0-9._\\-]+)");
    private static final Pattern JWT = Pattern.compile("(eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+)");

    private final SharedPreferences prefs;
    private final EvidenceLogger logger;

    public GdTokenStore(@NonNull Context context, @NonNull EvidenceLogger logger) {
        this.prefs = context.getSharedPreferences(EnvironmentConfig.PREFS, Context.MODE_PRIVATE);
        this.logger = logger;
    }

    public void save(@NonNull String token) {
        prefs.edit().putString(KEY_GD_TOKEN, token.trim()).apply();
    }

    public void clear() {
        prefs.edit().remove(KEY_GD_TOKEN).apply();
    }

    @Nullable
    public String current() {
        String stored = prefs.getString(KEY_GD_TOKEN, "");
        return TextUtils.isEmpty(stored) ? null : stored;
    }

    /**
     * Prefs first, then harvest from device logcat (PCM websocket URL).
     */
    @Nullable
    public String obtain() {
        String stored = current();
        if (stored != null) {
            JwtInspector inspect = JwtInspector.inspect(stored);
            if (inspect.present && !inspect.expired) {
                logger.info("GD token from companion store " + inspect.summary);
                return stored;
            }
            logger.info("Stored GD token unusable (" + inspect.summary + "), trying logcat");
        }
        String harvested = harvestFromLogcat();
        if (harvested != null) {
            save(harvested);
            logger.pass("GD token harvested from PCM logcat " + JwtInspector.inspect(harvested).summary);
            return harvested;
        }
        logger.fail("GD token not present in companion store or PCM logcat");
        return null;
    }

    @Nullable
    private String harvestFromLogcat() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"logcat", "-d", "-v", "brief"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            String last = null;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Attempting to connect websocket:") || line.contains("?token=")) {
                    String token = extractToken(line);
                    if (token != null) {
                        last = token;
                    }
                }
            }
            return last;
        } catch (Exception e) {
            logger.info("Logcat token harvest unavailable: " + e.getMessage());
            return null;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    @Nullable
    private static String extractToken(@NonNull String line) {
        Matcher query = TOKEN_QUERY.matcher(line);
        if (query.find()) {
            return query.group(1);
        }
        Matcher jwt = JWT.matcher(line);
        if (jwt.find()) {
            return jwt.group(1);
        }
        return null;
    }
}
