#!/usr/bin/env python3
"""Evaluate iot-release-gate.json. Exit 0 PASS, 1 FAIL, 2 missing/SKIP/invalid."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path


def evaluate(path: Path) -> int:
    if not path.is_file() or path.stat().st_size == 0:
        print(f"missing or empty: {path}", file=sys.stderr)
        return 2
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        print(f"invalid JSON: {exc}", file=sys.stderr)
        return 2
    if data.get("skipped") is True or str(data.get("status", "")).upper() == "SKIP":
        print(data.get("message", "SKIP"))
        return 2
    ready = data.get("releaseReady")
    passed = data.get("passed")
    status = str(data.get("status", "")).upper()
    if ready is True and (passed is True or status == "PASS"):
        print(data.get("message", "PASS"))
        return 0
    print(data.get("message", "FAIL"), file=sys.stderr)
    return 1


def self_test(root: Path) -> int:
    fixtures = root / "scripts" / "release" / "fixtures"
    if evaluate(fixtures / "iot-release-gate-pass.json") != 0:
        print("self-test expected PASS fixture to exit 0", file=sys.stderr)
        return 1
    if evaluate(fixtures / "iot-release-gate-fail.json") != 1:
        print("self-test expected FAIL fixture to exit 1", file=sys.stderr)
        return 1
    if evaluate(fixtures / "iot-release-gate-skip.json") != 2:
        print("self-test expected SKIP fixture to exit 2", file=sys.stderr)
        return 1
    print("self-test ok")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("gate_json", nargs="?", type=Path)
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[2]
    if args.self_test:
        return self_test(root)
    if args.gate_json is None:
        parser.error("gate_json required unless --self-test")
    return evaluate(args.gate_json)


if __name__ == "__main__":
    sys.exit(main())
