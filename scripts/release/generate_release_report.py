#!/usr/bin/env python3
"""Write reports/iot-release-gate.html from iot-release-gate.json and append history."""
from __future__ import annotations

import argparse
import html
import json
import sys
from datetime import datetime, timezone
from pathlib import Path


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def render(data: dict) -> str:
    ready = bool(data.get("releaseReady"))
    status = str(data.get("status", "UNKNOWN")).upper()
    overall = data.get("message") or status
    css = "ready" if ready else ("skip" if status == "SKIP" else "blocked")
    rows = []
    for check in data.get("checks") or []:
        name = html.escape(str(check.get("name", "")))
        st = html.escape(str(check.get("status", "")))
        msg = html.escape(str(check.get("message", "")))
        rows.append(f"<tr><td>{name}</td><td class='{st.lower()}'>{st}</td><td>{msg}</td></tr>")
    table = "\n".join(rows) or "<tr><td colspan='3'>No checks</td></tr>"
    generated = html.escape(str(data.get("generatedAt") or datetime.now(timezone.utc).isoformat()))
    build = html.escape(str(data.get("buildId", "")))
    companion = html.escape(str(data.get("companionVersion", "")))
    production = html.escape(str(data.get("productionVersion", data.get("productionPackage", ""))))
    return f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
  <title>IoT Companion Release Gate</title>
  <style>
    body {{ margin:0; font-family: Georgia, serif; color:#10233f; background:#eef2f6; }}
    main {{ max-width:960px; margin:0 auto; padding:32px 24px; }}
    .brand {{ letter-spacing:.12em; text-transform:uppercase; color:#4a5d78; font-size:.8rem; }}
    h1 {{ margin:8px 0 16px; }}
    .overall {{ display:inline-block; padding:10px 14px; border-radius:4px; font-family:sans-serif; font-weight:700; }}
    .overall.ready {{ background:#d8f3e7; color:#0b6e4f; }}
    .overall.blocked {{ background:#fde2e1; color:#9b2226; }}
    .overall.skip {{ background:#fff3cd; color:#856404; }}
    table {{ width:100%; border-collapse:collapse; margin-top:24px; background:#fff; }}
    th, td {{ text-align:left; padding:10px 12px; border-bottom:1px solid #d5e0ec; }}
    td.pass {{ color:#0b6e4f; font-weight:700; }}
    td.fail {{ color:#9b2226; font-weight:700; }}
    td.skip {{ color:#856404; font-weight:700; }}
    .meta {{ color:#4a5d78; margin-top:12px; font-family:sans-serif; font-size:.9rem; }}
  </style>
</head>
<body>
  <main>
    <div class="brand">Poynt IoT Companion · Phase 6</div>
    <h1>Nightly IoT release gate</h1>
    <div class="overall {css}">{html.escape(str(overall))}</div>
    <p class="meta">buildId={build} · companion={companion} · production={production} · {generated}</p>
    <table>
      <thead><tr><th>Check</th><th>Status</th><th>Detail</th></tr></thead>
      <tbody>
        {table}
      </tbody>
    </table>
    <p class="meta">Raw GD tokens are never included in this report.</p>
  </main>
</body>
</html>
"""


def append_history(history: Path, data: dict, html_path: Path) -> None:
    history.parent.mkdir(parents=True, exist_ok=True)
    line = json.dumps({
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "buildId": data.get("buildId"),
        "status": data.get("status"),
        "releaseReady": data.get("releaseReady"),
        "message": data.get("message"),
        "html": str(html_path),
    }, separators=(",", ":"))
    with history.open("a", encoding="utf-8") as handle:
        handle.write(line + "\n")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("gate_json", type=Path)
    parser.add_argument("--out-dir", type=Path, default=None)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[2]
    out_dir = args.out_dir or (root / "reports")
    out_dir.mkdir(parents=True, exist_ok=True)
    data = load(args.gate_json)
    html_path = out_dir / "iot-release-gate.html"
    html_path.write_text(render(data), encoding="utf-8")
    latest = out_dir / "latest"
    latest.mkdir(parents=True, exist_ok=True)
    (latest / "iot-release-gate.json").write_text(json.dumps(data, indent=2), encoding="utf-8")
    append_history(out_dir / "history" / "summary.jsonl", data, html_path)
    print(html_path)
    return 0


if __name__ == "__main__":
    sys.exit(main())
