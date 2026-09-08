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

## Phase 5 — Automation (this branch)

`feature/companion-test-app-phase-5`

- Broadcast `RUN_ACTION` / `PHMP_GATE` on the live `IotController`
- Gate file `iot-phmp-gate.json` for PHMP (`status`, `passed`, `skipped`, `details`, `metrics`)
- Stable dashboard resource ids (`automation/ids.json`)
- Host scripts under `scripts/automation/`
- UIAutomator id smoke test (no MQTT)
- PHMP drop-in `phmp-dropin/IotCompanionValidator.java`

Details: [`docs/phase5-automation.md`](phase5-automation.md).

## Phase 6 — Release validation

Nightly PoyntOS → install production + companion → full IoT suite + negative suite → PHMP pass/fail.
