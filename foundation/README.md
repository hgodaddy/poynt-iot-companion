# foundation/poynt-cloudmessaging

This directory is the **production Cloud Messaging source**. It is not copied into git.

Clone it (requires GitHub access to `gdcorp-commerce/poynt-cloudmessaging`):

```bash
./scripts/clone-foundation.sh
./scripts/map-foundation.sh
```

From Cursor: Command Palette → **Git: Clone** → paste
`https://github.com/gdcorp-commerce/poynt-cloudmessaging.git` → choose this
`foundation/` folder, **or** ask the agent after `gh auth login` completes.

Do **not** commit certificates, private keys, or AWS credentials from that repo.
