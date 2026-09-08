# Phase 6 — Release validation

Nightly path: **PoyntOS image → production PCM + companion → `RELEASE_GATE` → pass/fail**.

This does not replace PHMP’s 11 PCM lifecycle checks. It is the IoT companion leg that PHMP can add as a critical check named **IoT Release Gate Decision**.

## Decision

`iot-release-gate.json` is release-ready only when every critical check **passed** and none were **skipped**:

| Check | Meaning |
|---|---|
| Production Cloud Messaging APK | `co.poynt.cloudmessaging` installed and enabled |
| Companion Test App | this APK is running |
| Device / PoyntOS | terminal identity present |
| IoT Companion Gate | full IoT flow **and** negative suite (SKIPPED negatives allowed) |

Host evaluator: **0** PASS, **1** FAIL, **2** missing / SKIP.

Raw GD tokens are never written.

## On device

```bash
adb shell am broadcast -a co.poynt.cloudmessaging.iot.test.RUN_ACTION \
  --es action RELEASE_GATE \
  --es buildId "$BUILD_ID" \
  --ei timeoutSec 360
```

`RELEASE_GATE` runs `PHMP_GATE` (flow + negatives) then writes `iot-release-gate.json` next to `iot-phmp-gate.json`.

## Host

```bash
# CI / no terminal — evaluators, HTML, assembleDebug
MODE=simulate ./scripts/release/run-nightly-gate.sh
bash scripts/release/verify-simulate.sh

# Lab terminal
MODE=device BUILD_ID=lab-$(date +%Y%m%d) \
  PRODUCTION_APK=/path/to/cloudmessaging.apk \
  GD_TOKEN='<jwt>' \
  ./scripts/release/run-nightly-gate.sh
```

`GD_TOKEN` is passed to `SET_GD_TOKEN` and is not echoed. Prefer injecting on the device over putting JWTs in CI logs.

Preflight (packages + Poynt properties): `./scripts/release/preflight.sh`

HTML: `reports/iot-release-gate.html`  
History: `reports/history/summary.jsonl`

## GitHub Actions

`.github/workflows/iot-nightly.yml` — 06:00 UTC simulate (evaluators + `assembleDebug`). `workflow_dispatch` mode `device` is a reminder to run the lab host script; GitHub-hosted runners have no Poynt terminal.

## PHMP

Copy:

- `phmp-dropin/IotCompanionValidator.java` — run the companion
- `phmp-dropin/IotReleaseGateEvaluator.java` — fold IoT critical names into the release decision

Add **IoT Companion Gate** (and/or **IoT Release Gate Decision**) to PHMP `CRITICAL` before claiming IoT ship.
