# Implementation phases

## Phase 1 — Foundation (done)

`feature/companion-test-app-phase-1`

## Phase 2 — IoT controls (done)

`feature/companion-test-app-phase-2`

Live Discover, GD token, MQTT5, pub/sub, reconnect, JSON export.

## Phase 3 — Diagnostics (this branch)

Persistent diagnostics for QA / PHMP:

- Network / Wi-Fi / internet probe (411 HTTPS RTT)
- MQTT state and attempt count
- Token state (fingerprint only)
- Stable error codes (`IOT-NET-*`, `IOT-DSC-*`, `IOT-TOK-*`, `IOT-MQTT-*`)
- Files: `iot-diagnostics.json`, `iot-companion-evidence.txt`, `iot-companion-result.json`

## Phase 4 — Negative testing

`feature/companion-test-app-phase-4`

## Phase 5 — Automation

Appium/UIAutomator on view ids, consume JSON in PHMP.

## Phase 6 — Release validation

Nightly PoyntOS → install production + companion → full IoT suite → PHMP pass/fail.
