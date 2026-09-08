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
 * Drop-in PHMP check. Copy next to {@code CloudMessagingValidator} and call from
 * {@code PcmHealthEngine} once you are ready to make IoT companion release-blocking.
 *
 * <p>Does not depend on this repo's Gradle graph. Uses host {@code adb} the same way
 * {@code scripts/automation/run-phmp-gate.sh} does. Align {@code CHECK_NAME} with
 * {@code AutomationContract.CHECK_NAME} / {@code iot-phmp-gate.json} {@code checkName}.
 *
 * <p>Shape matches {@code ValidationResult}: name, passed, skipped, message, details, metrics.
 * Wire by mapping {@link Result} onto PHMP {@code ValidationResult.pass/fail/skip}.
 */
public final class IotCompanionValidator {

    public static final String CHECK_NAME = "IoT Companion Gate";
    public static final String PACKAGE = "co.poynt.cloudmessaging.iot.test";
    public static final String GATE_FILE = "iot-phmp-gate.json";

    private final String adbSerial;
    private final int timeoutSec;

    public IotCompanionValidator() {
        this(null, 240);
    }

    public IotCompanionValidator(String adbSerial, int timeoutSec) {
        this.adbSerial = adbSerial;
        this.timeoutSec = timeoutSec;
    }

    public Result validate() {
        List<String> details = new ArrayList<>();
        try {
            List<String> rm = adbBase();
            rm.add("shell");
            rm.add("rm");
            rm.add("-f");
            rm.add("/sdcard/Android/data/" + PACKAGE + "/files/" + GATE_FILE);
            run(rm, 15);

            List<String> start = adbBase();
            start.add("shell");
            start.add("am");
            start.add("start");
            start.add("-n");
            start.add(PACKAGE + "/.ui.DashboardActivity");
            run(start, 30);

            List<String> broadcast = adbBase();
            broadcast.add("shell");
            broadcast.add("am");
            broadcast.add("broadcast");
            broadcast.add("-a");
            broadcast.add(PACKAGE + ".RUN_ACTION");
            broadcast.add("--es");
            broadcast.add("action");
            broadcast.add("PHMP_GATE");
            broadcast.add("--ei");
            broadcast.add("timeoutSec");
            broadcast.add(String.valueOf(timeoutSec));
            run(broadcast, 30);

            Path tmp = Files.createTempFile("iot-phmp-gate", ".json");
            long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSec + 30);
            String remote = "/sdcard/Android/data/" + PACKAGE + "/files/" + GATE_FILE;
            String json = null;
            while (System.currentTimeMillis() < deadline) {
                List<String> cat = adbBase();
                cat.add("exec-out");
                cat.add("sh");
                cat.add("-c");
                cat.add("cat " + remote);
                ExecResult pulled = run(cat, 20);
                if (pulled.exitCode == 0 && pulled.stdout.trim().startsWith("{")) {
                    json = pulled.stdout;
                    Files.writeString(tmp, json, StandardCharsets.UTF_8);
                    break;
                }
                Thread.sleep(5_000L);
            }
            if (json == null) {
                return Result.skip(CHECK_NAME, "iot-phmp-gate.json not produced in time", details);
            }
            details.add("gate=" + tmp);
            boolean passed = json.contains("\"passed\": true") || json.contains("\"status\": \"PASS\"");
            boolean skipped = json.contains("\"skipped\": true") || json.contains("\"status\": \"SKIP\"");
            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("companionPackage", PACKAGE);
            if (skipped) {
                return Result.skip(CHECK_NAME, "Companion gate SKIP", details);
            }
            if (passed) {
                return Result.pass(CHECK_NAME, "Companion PHMP_GATE PASS", details, metrics);
            }
            return Result.fail(CHECK_NAME, "Companion PHMP_GATE FAIL", details);
        } catch (Exception e) {
            details.add(String.valueOf(e.getMessage()));
            return Result.fail(CHECK_NAME, "Companion gate error: " + e.getMessage(), details);
        }
    }

    private List<String> adbBase() {
        List<String> cmd = new ArrayList<>();
        cmd.add("adb");
        if (adbSerial != null && !adbSerial.isBlank()) {
            cmd.add("-s");
            cmd.add(adbSerial);
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

    public static final class Result {
        public final String name;
        public final boolean passed;
        public final boolean skipped;
        public final String message;
        public final List<String> details;
        public final Map<String, Object> metrics;

        private Result(String name, boolean passed, boolean skipped, String message,
                       List<String> details, Map<String, Object> metrics) {
            this.name = name;
            this.passed = passed;
            this.skipped = skipped;
            this.message = message;
            this.details = details;
            this.metrics = metrics;
        }

        public static Result pass(String name, String message, List<String> details, Map<String, Object> metrics) {
            return new Result(name, true, false, message, details, metrics);
        }

        public static Result fail(String name, String message, List<String> details) {
            return new Result(name, false, false, message, details, Map.of());
        }

        public static Result skip(String name, String message, List<String> details) {
            return new Result(name, true, true, message, details, Map.of("skipped", true));
        }
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
