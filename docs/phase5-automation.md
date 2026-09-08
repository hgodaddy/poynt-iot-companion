# Phase 5 — Automation

Headless ADB is the primary path on Poynt terminals. Appium/UIAutomator use the same resource ids.

Stable catalog: [`automation/ids.json`](../automation/ids.json). Do not rename ids without updating that file and `AutomationContract`.

## On-device contract

| Item | Value |
|---|---|
| Package | `co.poynt.cloudmessaging.iot.test` |
| Dashboard | `co.poynt.cloudmessaging.iot.test/.ui.DashboardActivity` |
| Intent | `co.poynt.cloudmessaging.iot.test.DASHBOARD` extra `action` |
| Broadcast | `co.poynt.cloudmessaging.iot.test.RUN_ACTION` extras `action`, `timeoutSec` |
| Token inject | `co.poynt.cloudmessaging.iot.test.SET_GD_TOKEN` extra `token` |
| Gate file | `/sdcard/Android/data/co.poynt.cloudmessaging.iot.test/files/iot-phmp-gate.json` |

`PHMP_GATE` runs the full IoT flow, then the negative suite, then writes the gate JSON. Raw GD tokens are never written.

```bash
adb shell am start -n co.poynt.cloudmessaging.iot.test/.ui.DashboardActivity \
  -a co.poynt.cloudmessaging.iot.test.DASHBOARD --es action DIAGNOSTICS

adb shell am broadcast -a co.poynt.cloudmessaging.iot.test.RUN_ACTION \
  --es action PHMP_GATE --ei timeoutSec 240
```

`AutomationReceiver` uses `CompanionApp.facade().iot()` (the live controller), not a second instance.

## Host scripts

```bash
python3 scripts/automation/evaluate_phmp_gate.py --self-test
./scripts/automation/run-phmp-gate.sh          # default PHMP_GATE
./scripts/automation/run-phmp-gate.sh NEG_SUITE
APPIUM_URL=http://127.0.0.1:4723 python3 scripts/automation/appium-dashboard.py
```

Evaluator exit codes: **0** PASS, **1** FAIL, **2** missing / SKIP / invalid JSON.

## UIAutomator

Instrumented test `DashboardIdsTest` asserts dashboard and button ids, then taps Refresh. It does **not** run MQTT or `PHMP_GATE`.

```bash
./gradlew :companion-app:connectedDebugAndroidTest
```

## PHMP

Copy [`phmp-dropin/IotCompanionValidator.java`](../phmp-dropin/IotCompanionValidator.java) into PHMP and map `Result` onto `ValidationResult`. Check name is `IoT Companion Gate`.

Phase 6 folds that check into a nightly release decision: [`docs/phase6-release-gate.md`](phase6-release-gate.md).
