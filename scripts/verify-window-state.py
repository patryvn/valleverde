#!/usr/bin/env python3
"""Detect visible Android system overlays and classify transient SystemUI ANRs."""
from __future__ import annotations

import re
import sys
from pathlib import Path

BLOCKING_TOKENS = ("ImmersiveModeConfirmation", "Application Not Responding")
SYSTEMUI_ANR = "Application Not Responding: com.android.systemui"
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


def is_retryable_systemui_anr(dump: str) -> bool:
    """Only a visible SystemUI ANR is retryable; immersive prompts remain fatal."""
    blockers = visible_blocking_windows(dump)
    return bool(blockers) and all(SYSTEMUI_ANR in title for title in blockers)


def main() -> int:
    if len(sys.argv) == 3 and sys.argv[1] == "--retryable-systemui-anr":
        dump = Path(sys.argv[2]).read_text(encoding="utf-8", errors="replace")
        if is_retryable_systemui_anr(dump):
            print("Only a visible SystemUI ANR is present; the smoke test may wait and retry.")
            return 0
        print("Visible overlay is not a retryable SystemUI ANR.")
        return 1

    if len(sys.argv) != 2:
        print(
            "Usage: verify-window-state.py WINDOWS_DUMP | "
            "verify-window-state.py --retryable-systemui-anr WINDOWS_DUMP",
            file=sys.stderr,
        )
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
