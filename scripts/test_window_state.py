import importlib.util
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).with_name("verify-window-state.py")
SPEC = importlib.util.spec_from_file_location("verify_window_state", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


class WindowStateTests(unittest.TestCase):
    def test_hidden_systemui_anr_does_not_block_capture(self):
        dump = """  Window #3 Window{abc u0 Application Not Responding: com.android.systemui}:
    mPolicyVisibility=false
    Surface: shown=false      mDrawState=READY_TO_SHOW
    isOnScreen=false
    isVisible=false
  Window #4 Window{def u0 MainActivity}:
    Surface: shown=true
"""
        self.assertEqual([], MODULE.visible_blocking_windows(dump))
        self.assertFalse(MODULE.is_retryable_systemui_anr(dump))

    def test_visible_systemui_anr_blocks_capture_but_is_retryable(self):
        dump = """  Window #3 Window{abc u0 Application Not Responding: com.android.systemui}:
    Surface: shown=true      mDrawState=HAS_DRAWN
"""
        self.assertEqual(1, len(MODULE.visible_blocking_windows(dump)))
        self.assertTrue(MODULE.is_retryable_systemui_anr(dump))

    def test_visible_game_anr_is_not_retryable(self):
        dump = """  Window #3 Window{abc u0 Application Not Responding: es.valledecernego.game}:
    Surface: shown=true
"""
        self.assertFalse(MODULE.is_retryable_systemui_anr(dump))

    def test_visible_immersive_confirmation_blocks_and_is_not_retryable(self):
        dump = """  Window #2 Window{abc u0 ImmersiveModeConfirmation}:
    Surface: shown=true
"""
        self.assertEqual(1, len(MODULE.visible_blocking_windows(dump)))
        self.assertFalse(MODULE.is_retryable_systemui_anr(dump))

    def test_mixed_systemui_anr_and_immersive_confirmation_is_not_retryable(self):
        dump = """  Window #2 Window{abc u0 Application Not Responding: com.android.systemui}:
    Surface: shown=true
  Window #3 Window{def u0 ImmersiveModeConfirmation}:
    Surface: shown=true
"""
        self.assertFalse(MODULE.is_retryable_systemui_anr(dump))


    def test_capture_decision_retries_systemui_overlay_race(self):
        dump = """  Window #3 Window{abc u0 Application Not Responding: com.android.systemui}:
    Surface: shown=true
"""
        self.assertEqual("retry", MODULE.capture_decision(dump, False))
        self.assertEqual("retry", MODULE.capture_decision(dump, True))

    def test_capture_decision_retries_failed_pixels_after_overlay_disappears(self):
        dump = """  Window #4 Window{def u0 MainActivity}:
    Surface: shown=true
"""
        self.assertEqual("retry", MODULE.capture_decision(dump, False))

    def test_capture_decision_accepts_clear_humanoid_capture(self):
        dump = """  Window #4 Window{def u0 MainActivity}:
    Surface: shown=true
"""
        self.assertEqual("accept", MODULE.capture_decision(dump, True))

    def test_capture_decision_rejects_non_retryable_visible_overlay(self):
        dump = """  Window #2 Window{abc u0 ImmersiveModeConfirmation}:
    Surface: shown=true
"""
        self.assertEqual("fatal", MODULE.capture_decision(dump, False))

    def run_capture_decision_cli(self, dump, pixels_valid):
        with tempfile.TemporaryDirectory() as directory:
            dump_path = Path(directory) / "windows.txt"
            dump_path.write_text(dump, encoding="utf-8")
            return subprocess.run(
                [sys.executable, str(SCRIPT), "--capture-decision",
                 str(dump_path), "1" if pixels_valid else "0"],
                text=True, capture_output=True, check=False,
            )

    def test_capture_decision_cli_accepts_clear_humanoid_frame(self):
        dump = """  Window #4 Window{def u0 MainActivity}:
    Surface: shown=true
"""
        result = self.run_capture_decision_cli(dump, True)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("Capture decision: accept", result.stdout)

    def test_capture_decision_cli_retries_pixel_race(self):
        dump = """  Window #4 Window{def u0 MainActivity}:
    Surface: shown=true
"""
        result = self.run_capture_decision_cli(dump, False)
        self.assertEqual(1, result.returncode, result.stderr)
        self.assertIn("Capture decision: retry", result.stdout)

    def test_capture_decision_cli_rejects_immersive_overlay(self):
        dump = """  Window #2 Window{abc u0 ImmersiveModeConfirmation}:
    Surface: shown=true
"""
        result = self.run_capture_decision_cli(dump, False)
        self.assertEqual(2, result.returncode, result.stderr)
        self.assertIn("Capture decision: fatal", result.stdout)

    def test_unrelated_visible_window_does_not_block_capture(self):
        dump = """  Window #1 Window{abc u0 MainActivity}:
    Surface: shown=true
"""
        self.assertEqual([], MODULE.visible_blocking_windows(dump))
        self.assertFalse(MODULE.is_retryable_systemui_anr(dump))


if __name__ == "__main__":
    unittest.main()
