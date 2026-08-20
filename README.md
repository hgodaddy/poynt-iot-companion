# Poynt IoT Companion Test App — Phase 1

QA control panel for **PST3 / P70** AWS IoT + Cloud Messaging testing. It is **not** a copy of the production app.

```text
Production APK (co.poynt.cloudmessaging)
        │  same terminal
        ▼
Companion APK (co.poynt.cloudmessaging.iot.test)  ← this repo
        │
        ▼  later: compile-time reuse
foundation/poynt-cloudmessaging  (cloned production source)
```

Phase 1 is the dashboard, device/PCM inspection, and production clone. Later `feature/companion-test-app-phase-*` branches add live IoT, diagnostics, and negative tests.

## Clone the production repo

```bash
gh auth login --web --hostname github.com
./scripts/clone-foundation.sh
./scripts/map-foundation.sh
```

Do not copy credentials, certificates, or production secrets from that clone into this app.

## Build and install

```bash
cd ~/Projects/poynt-iot-companion
./gradlew :companion-app:assembleDebug
adb install -r companion-app/build/outputs/apk/debug/companion-app-debug.apk
adb shell am start -n co.poynt.cloudmessaging.iot.test/.ui.DashboardActivity
```

See `docs/architecture.md` and `docs/phases.md`.
