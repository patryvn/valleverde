#!/usr/bin/env python3
"""Detect only visible Android system overlays that can invalidate a game screenshot."""
from __future__ import annotations

import re
import sys
from pathlib import Path

BLOCKING_TOKENS = ("ImmersiveModeConfirmation", "Application Not Responding")
WINDOW_HEADER = re.compile(r"^\\s*Window #\\d+ Window\\{")

def visible_blocking_windows(dump: str) -> list[str]:
    found: list[str] = []
    title: str | None = None
    visible = False

    def finish() -> None:
        if title and visible and any(token in title for token in BLOCKING_TOKENS):
            found.append(title.strip())

    for line in dump.splitlines():
        if WINDOW_HEADER.match(line):
            finish()
            title = line
            visible = False
        elif title is not None:
            if "Surface: shown=true" in line:
                visible = True
            elif "Surface: shown=false" in line:
                visible = False
    finish()
    return found

def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: verify-window-state.py WINDOWS_DUMP", file=sys.stderr)
        return 2
    dump = Path(sys.argv[1]).read_text(encoding="utf-8", errors="replace")
    visible = visible_blocking_windows(dump)
    if visible:
        print("Visible blocking Android system window(s):")
        for title in visible:
            print("  " + title)
        return 1
    print("No visible blocking Android system overlays (hidden windows ignored).")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
