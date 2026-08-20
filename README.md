# Poynt IoT Companion Test App — Phase 3

QA control panel for **PST3 / P70** AWS IoT + Cloud Messaging testing.

Phase 3 adds persistent diagnostics: network/Wi-Fi/internet, MQTT state, token state (fingerprint only), error codes, and `iot-diagnostics.json`.

```bash
git checkout feature/companion-test-app-phase-3
./gradlew :companion-app:assembleDebug
adb install -r companion-app/build/outputs/apk/debug/companion-app-debug.apk

adb exec-out sh -c 'cat /sdcard/Android/data/co.poynt.cloudmessaging.iot.test/files/iot-diagnostics.json'
```

See `docs/architecture.md` and `docs/phases.md`.
