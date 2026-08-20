# Implementation phases

## Phase 1 — Foundation (done)

`feature/companion-test-app-phase-1`

## Phase 2 — IoT controls (done)

`feature/companion-test-app-phase-2`

## Phase 3 — Diagnostics (done)

`feature/companion-test-app-phase-3`

## Phase 4 — Negative testing (this branch)

PASS means the companion **correctly detected** the failure. SKIPPED is allowed (suite still PASS). Overall FAIL if any scenario FAILs.

| Group | Scenarios |
|---|---|
| Token | missing store, expired JWT, invalid JWT (restores previous token) |
| Discover | timeout (`192.0.2.1`), HTTP 404 path, duplicate in-flight, invalid host |
| MQTT | publish/subscribe while disconnected, junk JWT connect, invalid host timeout |
| Network | blackhole `192.0.2.1:443`; Wi-Fi off/on (SKIPPED with `adb shell svc wifi disable` if the APK cannot toggle) |

Results: `iot-negative-results.json`.

## Phase 5 — Automation

Appium/UIAutomator on view ids, consume `iot-companion-result.json` / `iot-negative-results.json` in PHMP.

## Phase 6 — Release validation

Nightly PoyntOS → install production + companion → full IoT suite + negative suite → PHMP pass/fail.
