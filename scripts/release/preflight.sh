#!/usr/bin/env bash
# Host preflight: production PCM + companion + a few Poynt properties. Never prints tokens.
set -euo pipefail

PROD="${PRODUCTION_PACKAGE:-co.poynt.cloudmessaging}"
COMP="${COMPANION_PACKAGE:-co.poynt.cloudmessaging.iot.test}"
ADB=(adb)
if [[ -n "${ANDROID_SERIAL:-}" ]]; then
  ADB=(adb -s "${ANDROID_SERIAL}")
fi

echo "adb devices:"
"${ADB[@]}" devices -l

echo "Production ${PROD}:"
"${ADB[@]}" shell pm path "${PROD}" || echo "MISSING ${PROD}"
"${ADB[@]}" shell dumpsys package "${PROD}" | grep -E 'versionName=|versionCode=|userId=|pkgFlags=' | head -n 20 || true

echo "Companion ${COMP}:"
"${ADB[@]}" shell pm path "${COMP}" || echo "MISSING ${COMP}"

echo "Poynt properties:"
"${ADB[@]}" shell getprop persist.poynt.pcm.discovery || true
"${ADB[@]}" shell getprop persist.poynt.srvc.url.pcm || true
"${ADB[@]}" shell getprop ro.product.model || true
"${ADB[@]}" shell getprop ro.build.version.poynt || true
