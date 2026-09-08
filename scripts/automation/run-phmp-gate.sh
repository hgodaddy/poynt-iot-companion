#!/usr/bin/env bash
# Run PHMP_GATE on a connected device, pull iot-phmp-gate.json, evaluate exit code.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
PKG="co.poynt.cloudmessaging.iot.test"
ACTION="${1:-PHMP_GATE}"
TIMEOUT_SEC="${TIMEOUT_SEC:-240}"
POLL_SEC="${POLL_SEC:-5}"
FILES="/sdcard/Android/data/${PKG}/files"
GATE="${FILES}/iot-phmp-gate.json"
OUT_DIR="${OUT_DIR:-${ROOT}/build/automation}"
ADB=(adb)
if [[ -n "${ANDROID_SERIAL:-}" ]]; then
  ADB=(adb -s "${ANDROID_SERIAL}")
fi

mkdir -p "${OUT_DIR}"

echo "Starting dashboard…"
"${ADB[@]}" shell am start -n "${PKG}/.ui.DashboardActivity" \
  -a "${PKG}.DASHBOARD" >/dev/null || true
"${ADB[@]}" shell rm -f "${GATE}" >/dev/null 2>&1 || true
sleep 2

echo "Broadcast RUN_ACTION ${ACTION} timeoutSec=${TIMEOUT_SEC}"
"${ADB[@]}" shell am broadcast \
  -a "${PKG}.RUN_ACTION" \
  --es action "${ACTION}" \
  --ei timeoutSec "${TIMEOUT_SEC}"

echo "Waiting up to ${TIMEOUT_SEC}s for ${GATE}"
deadline=$((SECONDS + TIMEOUT_SEC + 30))
while (( SECONDS < deadline )); do
  if "${ADB[@]}" exec-out sh -c "test -s '${GATE}' && cat '${GATE}'" > "${OUT_DIR}/iot-phmp-gate.json" 2>/dev/null; then
    if python3 "${ROOT}/scripts/automation/evaluate_phmp_gate.py" "${OUT_DIR}/iot-phmp-gate.json"; then
      echo "PHMP gate PASS"
      exit 0
    else
      code=$?
      echo "Gate file present, evaluator exit ${code}"
      if [[ "${code}" -eq 2 ]]; then
        sleep "${POLL_SEC}"
        continue
      fi
      "${ADB[@]}" exec-out sh -c "cat '${FILES}/iot-companion-result.json'" > "${OUT_DIR}/iot-companion-result.json" || true
      "${ADB[@]}" exec-out sh -c "cat '${FILES}/iot-negative-results.json'" > "${OUT_DIR}/iot-negative-results.json" || true
      exit "${code}"
    fi
  fi
  sleep "${POLL_SEC}"
done

echo "Timed out waiting for ${GATE}" >&2
exit 2
