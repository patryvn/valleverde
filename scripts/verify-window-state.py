#!/usr/bin/env python3
"""Detect visible Android system overlays and classify transient SystemUI ANRs."""
from __future__ import annotations

import re
import sys
from pathlib import Path

BLOCKING_TOKENS = ("ImmersiveModeConfirmation", "Application Not Responding")
SYSTEMUI_ANR = "Application Not Responding: com.android.systemui"
WINDOW_HEADER = re.compile(r"^\s*Window #\d+ Window\{")


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



def app_window_is_visible(dump: str, package: str) -> bool:
    """Require the target app's own window to have a visible, drawn surface."""
    if not package.strip():
        return False
    title: str | None = None
    block: list[str] = []

    def matches_visible_app() -> bool:
        if title is None or package not in title:
            return False
        content = "\n".join(block)
        return (
            "mViewVisibility=0x0" in content
            and "mHasSurface=true" in content
            and "Surface: shown=true" in content
        )

    for line in dump.splitlines():
        if WINDOW_HEADER.match(line):
            if matches_visible_app():
                return True
            title = line
            block = [line]
        elif title is not None:
            block.append(line)
    return matches_visible_app()

def is_retryable_systemui_anr(dump: str) -> bool:
    """Only a visible SystemUI ANR is retryable; immersive prompts remain fatal."""
    blockers = visible_blocking_windows(dump)
    return bool(blockers) and all(SYSTEMUI_ANR in title for title in blockers)



def capture_decision(dump: str, screenshot_valid: bool) -> str:
    """Accept clean captures, retry transient SystemUI ANRs or pixel races, reject other overlays."""
    blockers = visible_blocking_windows(dump)
    if blockers:
        return "retry" if is_retryable_systemui_anr(dump) else "fatal"
    return "accept" if screenshot_valid else "retry"


def systemui_anr_dismiss_point(dump: str, width: int, height: int) -> tuple[int, int] | None:
    """Return the AVD's SystemUI ANR 'Close' tap point only for that visible overlay."""
    if width <= 0 or height <= 0 or not is_retryable_systemui_anr(dump):
        return None
    return round(width * 0.63), round(height * 0.45)


def main() -> int:
    if len(sys.argv) == 5 and sys.argv[1] == "--dismiss-point":
        dump = Path(sys.argv[2]).read_text(encoding="utf-8", errors="replace")
        try:
            width, height = int(sys.argv[3]), int(sys.argv[4])
        except ValueError:
            print("Display width and height must be integers.", file=sys.stderr)
            return 2
        point = systemui_anr_dismiss_point(dump, width, height)
        if point is None:
            print("No safe SystemUI ANR dismiss point.", file=sys.stderr)
            return 1
        print(f"{point[0]} {point[1]}")
        return 0

    if len(sys.argv) == 4 and sys.argv[1] == "--capture-decision":
        dump = Path(sys.argv[2]).read_text(encoding="utf-8", errors="replace")
        screenshot_valid = sys.argv[3] == "1"
        decision = capture_decision(dump, screenshot_valid)
        print("Capture decision: " + decision)
        return {"accept": 0, "retry": 1, "fatal": 2}[decision]

    if len(sys.argv) == 4 and sys.argv[1] == "--app-visible":
        dump = Path(sys.argv[2]).read_text(encoding="utf-8", errors="replace")
        package = sys.argv[3]
        if app_window_is_visible(dump, package):
            print(f"Visible app surface confirmed for {package}.")
            return 0
        print(f"No visible, drawn app surface found for {package}.")
        return 1

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
            "verify-window-state.py --retryable-systemui-anr WINDOWS_DUMP | "
            "verify-window-state.py --capture-decision WINDOWS_DUMP PIXELS_VALID(0|1) | "
            "verify-window-state.py --dismiss-point WINDOWS_DUMP WIDTH HEIGHT | "
            "verify-window-state.py --app-visible WINDOWS_DUMP PACKAGE",
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
