# Poynt IoT Companion Test App — Phase 6

QA control panel for **PST3 / P70** AWS IoT + Cloud Messaging testing.

Phase 6 is the **nightly release gate**: production PCM + companion → full IoT flow + negative suite → pass/fail. Simulate CI does not need a terminal; lab `MODE=device` does.

```bash
git checkout feature/companion-test-app-phase-6
MODE=simulate ./scripts/release/run-nightly-gate.sh
bash scripts/release/verify-simulate.sh

adb install -r companion-app/build/outputs/apk/debug/companion-app-debug.apk
MODE=device BUILD_ID=lab-$(date +%Y%m%d) ./scripts/release/run-nightly-gate.sh
```

Headless:

```bash
adb shell am broadcast -a co.poynt.cloudmessaging.iot.test.RUN_ACTION \
  --es action RELEASE_GATE --es buildId "$BUILD_ID" --ei timeoutSec 360
```

Artifacts: `reports/iot-release-gate.html`, on-device `iot-release-gate.json`. Never contains a raw GD token.

Stacked branches:

- `feature/companion-test-app-phase-1` — foundation
- `feature/companion-test-app-phase-2` — live IoT controls
- `feature/companion-test-app-phase-3` — diagnostics
- `feature/companion-test-app-phase-4` — negative tests
- `feature/companion-test-app-phase-5` — automation
- `feature/companion-test-app-phase-6` — release gate (this branch)

See `docs/phase6-release-gate.md`, `docs/phase5-automation.md`, and `docs/phases.md`.
