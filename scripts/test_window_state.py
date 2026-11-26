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

    def test_visible_systemui_anr_blocks_capture(self):
        dump = """  Window #3 Window{abc u0 Application Not Responding: com.android.systemui}:
    Surface: shown=true      mDrawState=HAS_DRAWN
"""
        self.assertEqual(1, len(MODULE.visible_blocking_windows(dump)))

    def test_visible_immersive_confirmation_blocks_capture(self):
        dump = """  Window #2 Window{abc u0 ImmersiveModeConfirmation}:
    Surface: shown=true
"""
        self.assertEqual(1, len(MODULE.visible_blocking_windows(dump)))

    def test_unrelated_visible_window_does_not_block_capture(self):
        dump = """  Window #1 Window{abc u0 MainActivity}:
    Surface: shown=true
"""
        self.assertEqual([], MODULE.visible_blocking_windows(dump))

if __name__ == "__main__":
    unittest.main()
