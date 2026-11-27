import unittest
from pathlib import Path

SCRIPT = Path(__file__).with_name("emulator-smoke.sh").read_text(encoding="utf-8")


class EmulatorSmokeInputTest(unittest.TestCase):
    def test_primary_touchscreen_swipe_targets_primary_display(self):
        self.assertIn("input touchscreen -d 0 swipe", SCRIPT)

    def test_fallback_swipe_targets_primary_display(self):
        self.assertIn("input -d 0 swipe", SCRIPT)

    def test_render_mode_tap_targets_primary_display(self):
        self.assertIn('input -d 0 tap "$render_x" "$render_y"', SCRIPT)

    def test_preview_launcher_label_identifies_the_3d_diagnostic_build(self):
        manifest = Path(__file__).parents[1] / "app/src/main/AndroidManifest.xml"
        self.assertIn('android:label="Valle de Cernego · Prueba 3D"', manifest.read_text(encoding="utf-8"))

    def test_release_smoke_uses_a_side_by_side_preview_package(self):
        self.assertIn('APP_ID="es.valledecernego.game.preview"', SCRIPT)
        self.assertIn('ACTIVITY_CLASS="es.valledecernego.game.MainActivity"', SCRIPT)
        self.assertIn('am start -n "$APP_ID/$ACTIVITY_CLASS"', SCRIPT)

    def test_smoke_test_still_requires_joystick_handler_events(self):
        self.assertIn("HUD_TOUCH DOWN .*mode=1", SCRIPT)
        self.assertIn("HUD_TOUCH MOVE .*mode=1", SCRIPT)

    def test_each_retry_requires_fresh_touch_events_not_stale_log_lines(self):
        self.assertIn("fresh_log_lines()", SCRIPT)
        self.assertIn('gesture_log_start="$(wc -l < emulator-output/renderer-live.log)"', SCRIPT)
        self.assertIn('new_log_lines="$(fresh_log_lines "$gesture_log_start")"', SCRIPT)

    def test_motion_telemetry_must_be_newer_than_the_gesture(self):
        self.assertIn('state_log_start="$gesture_log_start"', SCRIPT)
        self.assertIn('fresh_log_lines "$state_log_start"', SCRIPT)

    def test_motion_failure_still_fails_the_smoke_job(self):
        self.assertIn('if [[ "$movement_ok" != "1" ]]', SCRIPT)
        self.assertIn("Player movement or camera follow verification failed", SCRIPT)
        self.assertIn('exit 1', SCRIPT)

    def test_new_session_defaults_to_rounded_alt_and_preserves_choice_across_egl_recreation(self):
        policy = Path(__file__).parents[1] / "app/src/main/java/es/valledecernego/game/PlayerRenderPolicy.java"
        activity = Path(__file__).parents[1] / "app/src/main/java/es/valledecernego/game/MainActivity.java"
        self.assertIn("DEFAULT_COMPATIBILITY_MODE = true", policy.read_text(encoding="utf-8"))
        source = activity.read_text(encoding="utf-8")
        self.assertIn("forceCompatibilityPlayerModel=PlayerRenderPolicy.DEFAULT_COMPATIBILITY_MODE", source)
        self.assertNotIn("forceCompatibilityPlayerModel=false;", source)

    def test_pixel_verifier_requires_both_arms_and_both_legs_not_only_a_blue_torso(self):
        verifier = Path(__file__).with_name("verify-player-screenshot.py").read_text(encoding="utf-8")
        for limb in ("left_arm", "right_arm", "left_leg", "right_leg"):
            self.assertIn('"{}": 80'.format(limb), verifier)
        self.assertIn("A single blue rectangle can satisfy the old torso-colour check", verifier)

    def test_smoke_captures_and_moves_both_default_alt_and_detailed_3d_models(self):
        self.assertIn('PLAYER_COMPATIBILITY geometry=rounded-11 GL=0x0', SCRIPT)
        self.assertIn('valle-cernego-alt.png', SCRIPT)
        self.assertIn('valle-cernego-alt-moving.png', SCRIPT)
        self.assertIn('PLAYER_MODE user-selected=detailed-60', SCRIPT)
        self.assertIn('valle-cernego-detailed.png', SCRIPT)
        self.assertIn('valle-cernego-detailed-moving.png', SCRIPT)
        self.assertIn('verify-player-motion.py "$initial_telemetry" "$moving_telemetry"', SCRIPT)

    def test_smoke_waits_for_systemui_recovery_before_tapping_render_mode(self):
        wait = SCRIPT.index('wait_for_clear_windows "before detailed mode toggle"')
        tap = SCRIPT.index('adb shell input -d 0 tap "$render_x" "$render_y"')
        self.assertLess(wait, tap)

    def test_cpu_only_emulator_uses_a_longer_swipe_without_lowering_motion_floor(self):
        self.assertIn('swipe "$joy_x" "$joy_y" "$move_x" "$joy_y" 4000', SCRIPT)
        motion_script = Path(__file__).with_name("verify-player-motion.py").read_text(encoding="utf-8")
        self.assertIn("MIN_MOVEMENT_METERS = 0.25", motion_script)


if __name__ == "__main__":
    unittest.main()
