package co.poynt.iot.companion.shared.logging;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Timestamped QA evidence. Phase 1 keeps this in memory and on-screen.
 * Phase 3 persists files; Phase 5 emits machine-readable JSON for PHMP.
 */
public final class EvidenceLogger {

    private static final int MAX_ENTRIES = 400;
    private final CopyOnWriteArrayList<String> entries = new CopyOnWriteArrayList<>();
    private final SimpleDateFormat timeFormat;

    public EvidenceLogger() {
        timeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
        timeFormat.setTimeZone(TimeZone.getDefault());
    }

    public void info(@NonNull String message) {
        append("INFO", message);
    }

    public void pass(@NonNull String message) {
        append("PASS", message);
    }

    public void fail(@NonNull String message) {
        append("FAIL", message);
    }

    public void pending(@NonNull String message) {
        append("PEND", message);
    }

    @NonNull
    public List<String> snapshot() {
        List<String> copy = new ArrayList<>(entries);
        Collections.reverse(copy);
        return copy;
    }

    @NonNull
    public String asText() {
        StringBuilder builder = new StringBuilder();
        for (String line : snapshot()) {
            builder.append(line).append('\n');
        }
        return builder.toString().trim();
    }

    public void clear() {
        entries.clear();
    }

    private void append(@NonNull String level, @NonNull String message) {
        String line = timeFormat.format(new Date()) + "  " + level + "  " + message;
        entries.add(line);
        while (entries.size() > MAX_ENTRIES) {
            entries.remove(0);
        }
    }
}
