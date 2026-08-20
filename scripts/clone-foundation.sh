#!/usr/bin/env bash
# Clone gdcorp-commerce/poynt-cloudmessaging into foundation/ without
# copying secrets. Requires GitHub auth (gh auth login or SSH).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/foundation/poynt-cloudmessaging"
REPO_HTTPS="https://github.com/gdcorp-commerce/poynt-cloudmessaging.git"
REPO_SSH="git@github.com:gdcorp-commerce/poynt-cloudmessaging.git"

mkdir -p "$ROOT/foundation"

if [[ -d "$DEST/.git" ]]; then
  echo "Foundation already cloned at $DEST"
  git -C "$DEST" fetch --all --prune
  git -C "$DEST" status -sb
  exit 0
fi

if [[ -d "$DEST" && -n "$(ls -A "$DEST" 2>/dev/null || true)" ]]; then
  echo "Refusing to clone: $DEST already exists and is not a git repo."
  exit 1
fi

clone_with() {
  local url="$1"
  echo "Cloning $url"
  git clone "$url" "$DEST"
}

if command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then
  echo "Using GitHub CLI credentials"
  gh repo clone gdcorp-commerce/poynt-cloudmessaging "$DEST"
elif git ls-remote "$REPO_SSH" HEAD >/dev/null 2>&1; then
  clone_with "$REPO_SSH"
else
  clone_with "$REPO_HTTPS"
fi

git -C "$DEST" checkout -B feature/companion-test-app
echo
echo "Cloned. Next: ./scripts/map-foundation.sh"
echo "Do not copy credentials, certificates, or production secrets from that tree into this app."
