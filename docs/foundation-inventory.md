# Foundation inventory

Generated after cloning `gdcorp-commerce/poynt-cloudmessaging` into `foundation/poynt-cloudmessaging`.

- Remote: `https://github.com/gdcorp-commerce/poynt-cloudmessaging.git`
- HEAD at clone: `dbfbef200` — *Update version for develop : 26040005*
- Gradle module: `:PoyntCloudMessaging` → `apps/PoyntCloudMessaging`
- Production package: `co.poynt.cloudmessaging`
- Production service: `co.poynt.cloudmessaging/.PcmService`

Do **not** copy `credentials.gradle`, `dump.rdb`, `download_app_secrets`, or anything from Secrets Manager into the companion.

## Technical dependency map

```text
PcmService.onCreate / onStartCommand
        ↓
iotClientManager.start()
        ↓
shouldAttemptIoT()?   (Build.MODEL in {P70}  OR  SharedPrefs iot_enabled_v2)
        ↓
411 Discover  (IoTDiscoveryManager → mothershipUrl + "discover")
        ↓
isIoTEnabled from discovery (iotEnabledFromDiscovery)
        ↓
GDAuthTokenManager  (IPoyntTokenService AIDL)
        ↓
persist connection config  (IoTSharedPrefsManager)
        ↓
AWSIoTClient.connect()  (MQTT5 + custom authorizer JWT)
        ↓
subscribe cloudMessages/{deviceId} + jobs/{deviceId}
        ↓
publish DEVICE_AUTHENTICATED on deviceMessages/{deviceId}
        ↓
IoTMessageRouter → PcmService (IIoTMessageHandler)
        ↓
onDisconnect → reconnect with valid token or refresh token
```

## Reusable production classes (Phase 2 bind targets)

| Concern | Class | Path |
|---|---|---|
| Orchestration | `IoTClientManager` | `apps/PoyntCloudMessaging/src/main/java/co/poynt/cloudmessaging/aws/` |
| MQTT5 client | `AWSIoTClient` | `.../aws/iot/` |
| Connection config | `IoTConnectionConfig` | `.../aws/iot/` |
| Discover API | `IoTDiscoveryManager` + `IoTDiscoveryService` | `.../aws/discovery/` |
| Discover model | `DiscoveredIoTServices` | `.../aws/discovery/model/` |
| GD token | `GDAuthTokenManager` | `.../aws/` |
| Persistence | `IoTSharedPrefsManager` | `.../aws/` |
| Incoming messages | `IoTMessageRouter` + `MessageAdapter` | `.../aws/` |
| Device identity | `PoyntDeviceMetaData` (OS lib) | client id = store device id, serial from metadata |
| Entry point | `PcmService` | starts IoT after PCM websocket path |
| DI wiring | `PcmModule` | Dagger providers for all of the above |
| Constants | `IoTConstants` | endpoints, topics, **hardcoded supported model = P70 only** |

## Answers to the Phase 1 questions

| Question | Answer in this tree |
|---|---|
| Where does IoT start? | `PcmService` sets the IoT message handler in `onCreate`, then `iotClientManager.start()` in `onStartCommand`. |
| How is the device identified? | MQTT client id = `PoyntDeviceMetaData.getStoreDeviceId()`. Discover also sends serial + OS incremental. Eligibility: `Build.MODEL` equals `P70`, or `iot_enabled_v2` in SharedPreferences. |
| How does Discover happen? | After 411 mothership URL: `IoTDiscoveryManager.discoverConfigs(mothershipUrl, deviceId, serial)` GET `{mothershipUrl}discover`. Retries 3× on HTTP 5xx. |
| Where is the token obtained? | `GDAuthTokenManager` binds `IPoyntTokenService` (`Intents.COMPONENT_POYNT_TOKEN_SERVICE`). |
| Where is it persisted? | JWT is held in memory on `IoTConnectionConfig`. Endpoint, authorizer, topics, `iot_enabled_v2`, `iot_connected` go through `IoTSharedPrefsManager` (`SharedPrefsWrapper`). |
| How is MQTT initialized? | `AWSIoTClient` — AWS CRT MQTT5 + `AwsIotMqtt5ClientBuilder`, custom authorizer `mothership-iot-authorizer`, token query key `token`. |
| How does authentication happen? | JWT from Poynt token service is set as the custom-authorizer token before `connect()`. |
| How are messages received? | `AWSIoTClient` publish events → `IoTClientManager.onMessage` → `IoTMessageRouter` → `PcmService`. |
| How does reconnect work? | On disconnect from `CONNECTED`: reuse valid token or `tokenManager.initialize()`. Config change from Discover restarts the connection. |

## PST3 vs P70

`IoTConstants.IOT_SUPPORTED_MODELS` is currently **`["P70"]` only**. PST3 is not in that list. IoT can still start on PST3 if Discover / SharedPreferences set `iot_enabled_v2`. Companion Phase 2 buttons should call `IoTClientManager.shouldAttemptIoT()` (via a thin facade), not invent a second eligibility rule.

## What Phase 2 should *not* do

- Include the entire `:PoyntCloudMessaging` app as the companion.
- Copy `credentials.gradle` or Secrets Manager download scripts.
- Hard-code the AWS IoT endpoints from `IoTConstants` into the companion APK (they are environment defaults in production; companion should use the same `PoyntEnv` path or external config).

Phase 2 should extract or compile against the `aws/` + `aws/iot/` + `aws/discovery/` packages and drive `IoTClientManager.start()` / `stop()` plus Discover from the dashboard buttons.
