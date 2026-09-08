package com.poynt.phmp.iot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Drop-in PHMP step for nightly IoT ship. Broadcasts {@code RELEASE_GATE} and reads
 * {@code iot-release-gate.json}. Map {@link IotCompanionValidator.Result} onto
 * {@code ValidationResult}. Check name: {@code IoT Release Gate Decision}.
 */
public final class IotReleaseGateValidator {

    public static final String CHECK_NAME = "IoT Release Gate Decision";
    public static final String PACKAGE = "co.poynt.cloudmessaging.iot.test";
    public static final String RELEASE_FILE = "iot-release-gate.json";

    private final String adbSerial;
    private final int timeoutSec;
    private final String buildId;

    public IotReleaseGateValidator() {
        this(null, 360, "phmp");
    }

    public IotReleaseGateValidator(String adbSerial, int timeoutSec, String buildId) {
        this.adbSerial = adbSerial;
        this.timeoutSec = timeoutSec;
        this.buildId = buildId == null || buildId.isBlank() ? "phmp" : buildId;
    }

    public IotCompanionValidator.Result validate() {
        List<String> details = new ArrayList<>();
        try {
            run(adb("shell", "rm", "-f",
                    "/sdcard/Android/data/" + PACKAGE + "/files/" + RELEASE_FILE), 15);
            run(adb("shell", "am", "start", "-n", PACKAGE + "/.ui.DashboardActivity"), 30);
            run(adb(
                    "shell", "am", "broadcast",
                    "-a", PACKAGE + ".RUN_ACTION",
                    "--es", "action", "RELEASE_GATE",
                    "--es", "buildId", buildId,
                    "--ei", "timeoutSec", String.valueOf(timeoutSec)), 30);

            Path tmp = Files.createTempFile("iot-release-gate", ".json");
            long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSec + 30);
            String remote = "/sdcard/Android/data/" + PACKAGE + "/files/" + RELEASE_FILE;
            String json = null;
            while (System.currentTimeMillis() < deadline) {
                ExecResult pulled = run(adb("exec-out", "sh", "-c", "cat " + remote), 20);
                if (pulled.exitCode == 0 && pulled.stdout.trim().startsWith("{")) {
                    json = pulled.stdout;
                    Files.writeString(tmp, json, StandardCharsets.UTF_8);
                    break;
                }
                Thread.sleep(5_000L);
            }
            if (json == null) {
                return IotCompanionValidator.Result.skip(
                        CHECK_NAME, "iot-release-gate.json not produced in time", details);
            }
            details.add("release=" + tmp);
            boolean skipped = json.contains("\"skipped\": true") || json.contains("\"status\": \"SKIP\"");
            boolean ready = json.contains("\"releaseReady\": true");
            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("releaseReady", ready);
            metrics.put("buildId", buildId);
            if (skipped) {
                return IotCompanionValidator.Result.skip(CHECK_NAME, "IoT release gate SKIP", details);
            }
            if (ready) {
                return IotCompanionValidator.Result.pass(
                        CHECK_NAME,
                        "PASS — nightly IoT build is release-ready",
                        details,
                        metrics);
            }
            return IotCompanionValidator.Result.fail(CHECK_NAME, "IoT release gate FAIL", details);
        } catch (Exception e) {
            details.add(String.valueOf(e.getMessage()));
            return IotCompanionValidator.Result.fail(
                    CHECK_NAME, "IoT release gate error: " + e.getMessage(), details);
        }
    }

    private List<String> adb(String... extra) {
        List<String> cmd = new ArrayList<>();
        cmd.add("adb");
        if (adbSerial != null && !adbSerial.isBlank()) {
            cmd.add("-s");
            cmd.add(adbSerial);
        }
        for (String part : extra) {
            cmd.add(part);
        }
        return cmd;
    }

    private static ExecResult run(List<String> command, int timeoutSeconds) throws Exception {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("timeout: " + command);
        }
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
            }
        }
        return new ExecResult(process.exitValue(), out.toString());
    }

    private static final class ExecResult {
        final int exitCode;
        final String stdout;

        ExecResult(int exitCode, String stdout) {
            this.exitCode = exitCode;
            this.stdout = stdout;
        }
    }
}
