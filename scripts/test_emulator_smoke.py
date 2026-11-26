import unittest
from pathlib import Path

SCRIPT = Path(__file__).with_name("emulator-smoke.sh").read_text(encoding="utf-8")


class EmulatorSmokeInputTest(unittest.TestCase):
    def test_primary_touchscreen_swipe_targets_primary_display(self):
        self.assertIn("input touchscreen -d 0 swipe", SCRIPT)

    def test_fallback_swipe_targets_primary_display(self):
        self.assertIn("input -d 0 swipe", SCRIPT)

    def test_release_smoke_uses_a_side_by_side_preview_package(self):
        self.assertIn('APP_ID="es.valledecernego.game.preview"', SCRIPT)
        self.assertIn('ACTIVITY_CLASS="es.valledecernego.game.MainActivity"', SCRIPT)
        self.assertIn('am start -n "$APP_ID/$ACTIVITY_CLASS"', SCRIPT)

    def test_smoke_test_still_requires_joystick_handler_events(self):
        self.assertIn("HUD_TOUCH DOWN .*mode=1", SCRIPT)
        self.assertIn("HUD_TOUCH MOVE .*mode=1", SCRIPT)


if __name__ == "__main__":
    unittest.main()
