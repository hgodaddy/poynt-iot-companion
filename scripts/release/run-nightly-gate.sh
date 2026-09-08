#!/usr/bin/env bash
# Nightly IoT release gate.
# MODE=simulate  — evaluator fixtures + assembleDebug + HTML (no device)
# MODE=device    — install APKs, RUN_ACTION RELEASE_GATE, evaluate
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
MODE="${MODE:-simulate}"
BUILD_ID="${BUILD_ID:-local-$(date -u +%Y%m%dT%H%M%SZ)}"
TIMEOUT_SEC="${TIMEOUT_SEC:-360}"
OUT_DIR="${OUT_DIR:-${ROOT}/reports}"
PROD_PKG="co.poynt.cloudmessaging"
COMP_PKG="co.poynt.cloudmessaging.iot.test"
FILES="/sdcard/Android/data/${COMP_PKG}/files"
RELEASE_REMOTE="${FILES}/iot-release-gate.json"
ADB=(adb)
if [[ -n "${ANDROID_SERIAL:-}" ]]; then
  ADB=(adb -s "${ANDROID_SERIAL}")
fi

mkdir -p "${OUT_DIR}"

run_self_tests() {
  python3 "${ROOT}/scripts/automation/evaluate_phmp_gate.py" --self-test
  python3 "${ROOT}/scripts/release/evaluate_release_gate.py" --self-test
}

assemble_companion() {
  export JAVA_HOME="${JAVA_HOME:-/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home}"
  if [[ ! -x "${JAVA_HOME}/bin/java" ]]; then
    unset JAVA_HOME
  fi
  (cd "${ROOT}" && ./gradlew :companion-app:assembleDebug)
}

simulate() {
  echo "MODE=simulate buildId=${BUILD_ID}"
  run_self_tests
  assemble_companion
  python3 "${ROOT}/scripts/release/generate_release_report.py" \
    "${ROOT}/scripts/release/fixtures/iot-release-gate-pass.json" \
    --out-dir "${OUT_DIR}"
  python3 "${ROOT}/scripts/release/evaluate_release_gate.py" \
    "${OUT_DIR}/latest/iot-release-gate.json"
  echo "SIMULATE PASS — APK built, evaluators green. Device-backed certification still required for ship."
}

wait_for_release_json() {
  local deadline=$((SECONDS + TIMEOUT_SEC + 30))
  while (( SECONDS < deadline )); do
    if "${ADB[@]}" exec-out sh -c "test -s '${RELEASE_REMOTE}' && cat '${RELEASE_REMOTE}'" \
        > "${OUT_DIR}/iot-release-gate.json" 2>/dev/null; then
      local code=0
      python3 "${ROOT}/scripts/release/evaluate_release_gate.py" "${OUT_DIR}/iot-release-gate.json" || code=$?
      if [[ "${code}" -eq 2 ]]; then
        sleep 5
        continue
      fi
      return "${code}"
    fi
    sleep 5
  done
  echo "Timed out waiting for ${RELEASE_REMOTE}" >&2
  return 2
}

pull_artifacts() {
  for name in iot-phmp-gate.json iot-companion-result.json iot-negative-results.json iot-diagnostics.json; do
    "${ADB[@]}" exec-out sh -c "cat '${FILES}/${name}'" > "${OUT_DIR}/${name}" 2>/dev/null || true
  done
  "${ADB[@]}" logcat -d -t 400 *:S IotCompanionAuto:I IotController:I \
      > "${OUT_DIR}/logcat-companion.txt" 2>/dev/null || true
}

device() {
  echo "MODE=device buildId=${BUILD_ID}"
  run_self_tests
  "${ADB[@]}" wait-for-device
  bash "${ROOT}/scripts/release/preflight.sh"

  if [[ -n "${PRODUCTION_APK:-}" ]]; then
    echo "Installing production ${PRODUCTION_APK}"
    "${ADB[@]}" install -r -d "${PRODUCTION_APK}"
  fi
  if ! "${ADB[@]}" shell pm path "${PROD_PKG}" >/dev/null 2>&1; then
    echo "Production ${PROD_PKG} is not installed. Set PRODUCTION_APK or flash PoyntOS first." >&2
    exit 1
  fi

  local apk="${COMPANION_APK:-${ROOT}/companion-app/build/outputs/apk/debug/companion-app-debug.apk}"
  if [[ ! -f "${apk}" ]]; then
    assemble_companion
  fi
  echo "Installing companion ${apk}"
  "${ADB[@]}" install -r "${apk}"

  if [[ -n "${GD_TOKEN:-}" ]]; then
    echo "Injecting GD token via broadcast (token not logged)"
    "${ADB[@]}" shell am broadcast -a "${COMP_PKG}.SET_GD_TOKEN" --es token "${GD_TOKEN}" >/dev/null
  fi

  "${ADB[@]}" shell am start -n "${COMP_PKG}/.ui.DashboardActivity" -a "${COMP_PKG}.DASHBOARD" >/dev/null || true
  "${ADB[@]}" shell rm -f "${RELEASE_REMOTE}" >/dev/null 2>&1 || true
  sleep 2
  echo "Broadcast RELEASE_GATE"
  "${ADB[@]}" shell am broadcast \
    -a "${COMP_PKG}.RUN_ACTION" \
    --es action RELEASE_GATE \
    --es buildId "${BUILD_ID}" \
    --ei timeoutSec "${TIMEOUT_SEC}"

  local code=0
  wait_for_release_json || code=$?
  pull_artifacts
  if [[ -f "${OUT_DIR}/iot-release-gate.json" ]]; then
    python3 "${ROOT}/scripts/release/generate_release_report.py" \
      "${OUT_DIR}/iot-release-gate.json" --out-dir "${OUT_DIR}" || true
  fi
  exit "${code}"
}

case "${MODE}" in
  simulate) simulate ;;
  device) device ;;
  *) echo "MODE must be simulate or device" >&2; exit 2 ;;
esac
