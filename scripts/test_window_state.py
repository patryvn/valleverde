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

    def test_app_window_requires_a_real_visible_surface(self):
        visible = """  Window #1 Window{abc u0 es.valledecernego.game.preview2/es.valledecernego.game.MainActivity}:
    mViewVisibility=0x0 mHaveFrame=true
    mHasSurface=true isReadyForDisplay()=true
    Surface: shown=true      mDrawState=HAS_DRAWN
"""
        sleeping = """  Window #1 Window{abc u0 es.valledecernego.game.preview2/es.valledecernego.game.MainActivity}:
    mViewVisibility=0x8 mHaveFrame=true
    mHasSurface=false isReadyForDisplay()=false
    Surface: shown=false     mDrawState=NO_SURFACE
"""
        self.assertTrue(MODULE.app_window_is_visible(visible, "es.valledecernego.game.preview2"))
        self.assertFalse(MODULE.app_window_is_visible(sleeping, "es.valledecernego.game.preview2"))

    def test_app_window_does_not_accept_another_package_surface(self):
        dump = """  Window #1 Window{abc u0 com.android.launcher3/.Launcher}:
    mViewVisibility=0x0 mHaveFrame=true
    mHasSurface=true isReadyForDisplay()=true
    Surface: shown=true      mDrawState=HAS_DRAWN
"""
        self.assertFalse(MODULE.app_window_is_visible(dump, "es.valledecernego.game.preview2"))

    def test_app_visible_cli_accepts_only_the_visible_game_surface(self):
        visible = """  Window #1 Window{abc u0 es.valledecernego.game.preview2/es.valledecernego.game.MainActivity}:
    mViewVisibility=0x0 mHaveFrame=true
    mHasSurface=true isReadyForDisplay()=true
    Surface: shown=true      mDrawState=HAS_DRAWN
"""
        sleeping = """  Window #1 Window{abc u0 es.valledecernego.game.preview2/es.valledecernego.game.MainActivity}:
    mViewVisibility=0x8 mHaveFrame=true
    mHasSurface=false isReadyForDisplay()=false
    Surface: shown=false     mDrawState=NO_SURFACE
"""
        with tempfile.TemporaryDirectory() as directory:
            dump_path = Path(directory) / "windows.txt"
            dump_path.write_text(visible, encoding="utf-8")
            accepted = subprocess.run(
                [sys.executable, str(SCRIPT), "--app-visible", str(dump_path),
                 "es.valledecernego.game.preview2"],
                text=True, capture_output=True, check=False,
            )
            self.assertEqual(0, accepted.returncode, accepted.stderr)
            dump_path.write_text(sleeping, encoding="utf-8")
            rejected = subprocess.run(
                [sys.executable, str(SCRIPT), "--app-visible", str(dump_path),
                 "es.valledecernego.game.preview2"],
                text=True, capture_output=True, check=False,
            )
            self.assertEqual(1, rejected.returncode, rejected.stderr)

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

    def test_systemui_anr_dismiss_point_targets_close_button_on_ci_display(self):
        dump = """  Window #3 Window{abc u0 Application Not Responding: com.android.systemui}:
    Surface: shown=true      mDrawState=HAS_DRAWN
"""
        self.assertEqual((454, 182), MODULE.systemui_anr_dismiss_point(dump, 720, 405))

    def test_systemui_anr_dismiss_point_is_not_returned_for_game_anr(self):
        dump = """  Window #3 Window{abc u0 Application Not Responding: es.valledecernego.game}:
    Surface: shown=true
"""
        self.assertIsNone(MODULE.systemui_anr_dismiss_point(dump, 720, 405))

    def test_systemui_anr_dismiss_point_rejects_invalid_display_size(self):
        dump = """  Window #3 Window{abc u0 Application Not Responding: com.android.systemui}:
    Surface: shown=true
"""
        self.assertIsNone(MODULE.systemui_anr_dismiss_point(dump, 0, 405))
        self.assertIsNone(MODULE.systemui_anr_dismiss_point(dump, 720, -1))


if __name__ == "__main__":
    unittest.main()
