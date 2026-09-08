# Poynt IoT Companion Test App — Phase 5

QA control panel for **PST3 / P70** AWS IoT + Cloud Messaging testing.

Phase 5 adds headless ADB (`RUN_ACTION` / `PHMP_GATE`), stable UIAutomator ids, host evaluators, and a PHMP drop-in. Raw GD tokens are never written to JSON.

```bash
git checkout feature/companion-test-app-phase-5
./gradlew :companion-app:assembleDebug
adb install -r companion-app/build/outputs/apk/debug/companion-app-debug.apk
adb shell am start -n co.poynt.cloudmessaging.iot.test/.ui.DashboardActivity

adb shell am broadcast -a co.poynt.cloudmessaging.iot.test.RUN_ACTION \
  --es action PHMP_GATE --ei timeoutSec 240

./scripts/automation/run-phmp-gate.sh
python3 scripts/automation/evaluate_phmp_gate.py --self-test
```

Stacked branches:

- `feature/companion-test-app-phase-1` — foundation
- `feature/companion-test-app-phase-2` — live IoT controls
- `feature/companion-test-app-phase-3` — diagnostics
- `feature/companion-test-app-phase-4` — negative tests
- `feature/companion-test-app-phase-5` — automation (this branch)

See `docs/phase5-automation.md`, `docs/architecture.md`, and `docs/phases.md`.
