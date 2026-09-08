#!/usr/bin/env bash
# CI / laptop: evaluators + optional HTML from fixtures. Does not require a terminal.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
python3 "${ROOT}/scripts/automation/evaluate_phmp_gate.py" --self-test
python3 "${ROOT}/scripts/release/evaluate_release_gate.py" --self-test
python3 "${ROOT}/scripts/release/generate_release_report.py" \
  "${ROOT}/scripts/release/fixtures/iot-release-gate-pass.json" \
  --out-dir "${ROOT}/reports"
python3 "${ROOT}/scripts/release/evaluate_release_gate.py" "${ROOT}/reports/latest/iot-release-gate.json"
echo "verify-simulate ok"
