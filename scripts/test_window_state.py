import importlib.util
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

    def test_unrelated_visible_window_does_not_block_capture(self):
        dump = """  Window #1 Window{abc u0 MainActivity}:
    Surface: shown=true
"""
        self.assertEqual([], MODULE.visible_blocking_windows(dump))
        self.assertFalse(MODULE.is_retryable_systemui_anr(dump))


if __name__ == "__main__":
    unittest.main()
