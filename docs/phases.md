# Implementation phases

## Phase 1 — Foundation (done)

`feature/companion-test-app-phase-1`

- Companion APK `co.poynt.cloudmessaging.iot.test`
- Dashboard, clone of `poynt-cloudmessaging`, class map

## Phase 2 — IoT controls (this branch)

Independent actions, each stoppable without repeating the others:

1. Device eligibility (`IOT_SUPPORTED_MODELS` = P70, plus PCM / iot hint)
2. Discover — 411 `/discovery/services` then mothership `discover`
3. GD token — companion store, logcat harvest, or ADB inject
4. MQTT5 connect — custom authorizer JWT
5. Subscribe — `cloudMessages/{id}`, `deviceMessages/{id}`, `jobs/{id}`
6. Publish — `DEVICE_AUTHENTICATED` plus companion correlation payload
7. Receive / validate — correlationId match
8. Disconnect
9. Reconnect
10. Export — `iot-companion-result.json` (no raw token)

Full flow stops on first FAIL.

## Phase 3 — Diagnostics

`feature/companion-test-app-phase-3`

## Phase 4 — Negative testing

`feature/companion-test-app-phase-4`

## Phase 5 — Automation

Appium/UIAutomator on view ids, consume JSON in PHMP.

## Phase 6 — Release validation

Nightly PoyntOS → install production + companion → full IoT suite → PHMP pass/fail.
