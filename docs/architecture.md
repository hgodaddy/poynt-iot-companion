# Architecture — Phase 1

## Principle

Reuse the production Cloud Messaging **implementation** for real device behavior. Keep QA controls in this companion. Do not fork-and-modify the production UI.

```text
                ┌─────────────────────────────┐
                │       Production App        │
                │  co.poynt.cloudmessaging    │
                │  (installed APK + source)   │
                └──────────────┬──────────────┘
                               │  shared foundation (Phase 2 bind)
                ┌──────────────▼──────────────┐
                │     Companion Test App      │
                │  co.poynt.cloudmessaging    │
                │           .iot.test         │
                │  dashboard · evidence log   │
                └──────────────┬──────────────┘
                               │
                ┌──────────────▼──────────────┐
                │     Test infrastructure     │
                │  AWS IoT · ADB · PHMP       │
                └─────────────────────────────┘
```

## Why a separate applicationId

`co.poynt.cloudmessaging.iot.test` can coexist with production on PST3/P70. That is the QA control panel.

Cross-UID SharedPreferences cannot see the production GD token. That is why Phase 2 must **compile against** extracted production IoT modules (or a sharedUserId privileged path), not scrape another app's private storage.

## Modules

| Module | Role |
|---|---|
| `companion-app` | Dashboard APK |
| `shared` | Device / PCM inspection, evidence log, environment config |
| `foundation/poynt-cloudmessaging` | Cloned production source (not compiled in Phase 1) |

## Production classes now in `foundation/poynt-cloudmessaging`

Clone completed. Phase 2 bind targets:

```text
PcmService
  → IoTClientManager.start()
      → IoTDiscoveryManager (mothership + "discover")
      → GDAuthTokenManager (IPoyntTokenService)
      → AWSIoTClient (MQTT5)
      → IoTSharedPrefsManager (iot_enabled_v2, endpoint, topics)
```

Full map: [`docs/foundation-inventory.md`](foundation-inventory.md).

## Known production facts (from PHMP / lab PST3 + this clone)

These are starting points for the foundation map, not a substitute for reading the clone:

- Package: `co.poynt.cloudmessaging`
- Service: `co.poynt.cloudmessaging/.PcmService`
- Shared UID: `android.uid.poynt`
- Discovery: `persist.poynt.pcm.discovery` (lab: `https://fouroneone.poynt.net/discovery/services`)
- PCM seed: `persist.poynt.srvc.url.pcm`
- Token rides the WebSocket URL today (`?token=`); AWS IoT / GD token is the new path to bind

## Technical dependency map (fill after clone)

```text
Where does IoT start?
        ↓
How is the device identified?
        ↓
How does Discover happen?
        ↓
Where is the token obtained?
        ↓
Where is it persisted?
        ↓
How is MQTT initialized?
        ↓
How does authentication happen?
        ↓
How are messages received?
        ↓
How does reconnect work?
```

Run `./scripts/map-foundation.sh` after clone. It writes `docs/foundation-inventory.md`.

## Secrets

Environment is `DEV` / `OTE` / `PROD`. Endpoints load from gitignored `config/*.properties` or device SharedPreferences. The APK must never hard-code AWS credentials or certificates.
