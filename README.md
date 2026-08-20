# Poynt IoT Companion Test App — Phase 2

QA control panel for **PST3 / P70** AWS IoT + Cloud Messaging testing. It is **not** a copy of the production app.

Phase 2 adds live IoT controls: Discover, GD token, MQTT5 custom-auth, subscribe, publish, receive, reconnect, and JSON export.

```bash
cd ~/Projects/poynt-iot-companion
git checkout feature/companion-test-app-phase-2
./gradlew :companion-app:assembleDebug
adb install -r companion-app/build/outputs/apk/debug/companion-app-debug.apk
adb shell am start -n co.poynt.cloudmessaging.iot.test/.ui.DashboardActivity

# Optional: inject a GD JWT (never commit this)
adb shell am broadcast -a co.poynt.cloudmessaging.iot.test.SET_GD_TOKEN --es token '<jwt>'

adb exec-out sh -c 'cat /sdcard/Android/data/co.poynt.cloudmessaging.iot.test/files/iot-companion-result.json'
```

See `docs/architecture.md` and `docs/phases.md`.
