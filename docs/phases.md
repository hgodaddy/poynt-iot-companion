# Implementation phases

## Phase 1 — Foundation (done)

`feature/companion-test-app-phase-1`

## Phase 2 — IoT controls (done)

`feature/companion-test-app-phase-2`

## Phase 3 — Diagnostics (done)

`feature/companion-test-app-phase-3`

## Phase 4 — Negative testing (done)

`feature/companion-test-app-phase-4`

PASS means the companion **correctly detected** the failure. SKIPPED is allowed (suite still PASS). Overall FAIL if any scenario FAILs.

## Phase 5 — Automation (done)

`feature/companion-test-app-phase-5`

Headless `RUN_ACTION` / `PHMP_GATE`, stable ids, host evaluators, PHMP drop-in. Details: [`docs/phase5-automation.md`](phase5-automation.md).

## Phase 6 — Release validation (this branch)

`feature/companion-test-app-phase-6`

Nightly PoyntOS → install production PCM + companion → `RELEASE_GATE` (flow + negatives + production preflight) → `iot-release-gate.json` / HTML → PHMP pass/fail.

- Simulate: GitHub Actions + `MODE=simulate ./scripts/release/run-nightly-gate.sh`
- Device: `MODE=device` on a lab host with ADB
- Drop-ins: `phmp-dropin/IotReleaseGateValidator.java`, `IotReleaseGateEvaluator.java`

Details: [`docs/phase6-release-gate.md`](phase6-release-gate.md).
