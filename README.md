# Poynt IoT Companion Test App — Phase 4

QA control panel for **PST3 / P70** AWS IoT + Cloud Messaging testing.

Phase 4 adds negative tests. **PASS means the companion correctly detected the failure.** SKIPPED (for example Wi-Fi toggle denied) does not fail the suite.

```bash
git checkout feature/companion-test-app-phase-4
./gradlew :companion-app:assembleDebug
adb install -r companion-app/build/outputs/apk/debug/companion-app-debug.apk
adb shell am start -n co.poynt.cloudmessaging.iot.test/.ui.DashboardActivity

adb exec-out sh -c 'cat /sdcard/Android/data/co.poynt.cloudmessaging.iot.test/files/iot-negative-results.json'
adb exec-out sh -c 'cat /sdcard/Android/data/co.poynt.cloudmessaging.iot.test/files/iot-diagnostics.json'
```

If Wi-Fi negative is SKIPPED:

```bash
adb shell svc wifi disable
adb shell svc wifi enable
```

Stacked branches:

- `feature/companion-test-app-phase-1` — foundation
- `feature/companion-test-app-phase-2` — live IoT controls
- `feature/companion-test-app-phase-3` — diagnostics
- `feature/companion-test-app-phase-4` — negative tests (this branch)

See `docs/architecture.md` and `docs/phases.md`.
