# Implementation phases

## Phase 1 — Foundation (this branch)

- Companion APK `co.poynt.cloudmessaging.iot.test`
- Dashboard, clone of `poynt-cloudmessaging`, class map
- Observe device + production APK; token / MQTT stay pending until Phase 2

## Phase 2 — IoT controls

`feature/companion-test-app-phase-2`

Live Discover, GD token, MQTT5, pub/sub, reconnect, JSON export.

## Phase 3 — Diagnostics

`feature/companion-test-app-phase-3`

Network / MQTT / token state, error codes, persistent diagnostics files.

## Phase 4 — Negative testing

`feature/companion-test-app-phase-4`

Token / Discover / MQTT / network failure detection (PASS = failure correctly detected).

## Phase 5 — Automation

Appium/UIAutomator on view ids, consume JSON in PHMP.

## Phase 6 — Release validation

Nightly PoyntOS → install production + companion → full IoT suite → PHMP pass/fail.
