# Cloning poynt-cloudmessaging from Cursor

The production repo is **private**. Cursor can clone it the same way it clones any GitHub repo, once GitHub credentials exist on this Mac.

## 1. Sign in to GitHub (one time)

In a Cursor terminal:

```bash
gh auth login --hostname github.com --git-protocol https --web
```

Complete the device code at https://github.com/login/device. Confirm `gh auth status` shows your user and access to `gdcorp-commerce`.

If your org uses SAML SSO, authorize `gdcorp-commerce` for the token when GitHub prompts.

## 2. Clone into this project's foundation slot

**Do not** clone into `~/Projects` as a second unrelated workspace if you want the companion to use it as a foundation layer. Put it here:

```text
poynt-iot-companion/
  foundation/
    poynt-cloudmessaging/   ← production source
  companion-app/
  shared/
```

From Cursor chat (after auth):

> clone the foundation now

Or Command Palette → **Git: Clone** →  
`https://github.com/gdcorp-commerce/poynt-cloudmessaging.git` →  
directory `…/poynt-iot-companion/foundation/poynt-cloudmessaging`

Or:

```bash
./scripts/clone-foundation.sh
./scripts/map-foundation.sh
```

## 3. Open both trees in one Cursor window (optional)

File → Add Folder to Workspace → `foundation/poynt-cloudmessaging`  
so production source and the companion sit side by side.

## 4. What not to do

- Do not clone onto `main` of production and commit QA UI there.
- Do not copy `.pem`, keystores, or AWS keys into `companion-app`.
- Do not set `FOUNDATION_BOUND=true` until mapped IoT modules compile into `shared`.

## After clone — Phase 1 remaining work

1. Read `docs/foundation-inventory.md`
2. Identify `IoTManager` / `MqttManager` / Discover / token classes
3. Include those Gradle modules from `settings.gradle.kts` (not the whole production app)
4. Flip `FOUNDATION_BOUND` and implement Phase 2 button wiring
