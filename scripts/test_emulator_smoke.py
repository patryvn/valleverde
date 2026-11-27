import unittest
from pathlib import Path

SCRIPT = Path(__file__).with_name("emulator-smoke.sh").read_text(encoding="utf-8")


class EmulatorSmokeInputTest(unittest.TestCase):
    def test_avd_display_is_kept_awake_for_long_swiftshader_captures(self):
        self.assertIn("settings put global stay_on_while_plugged_in 7", SCRIPT)
        self.assertIn("settings put system screen_off_timeout 1800000", SCRIPT)
        self.assertIn("svc power stayon true", SCRIPT)
        self.assertIn("input keyevent 224", SCRIPT)

    def test_capture_requires_the_games_own_visible_surface(self):
        self.assertIn('--app-visible "$dump_path" "$APP_ID"', SCRIPT)
        self.assertIn('--app-visible "$windows_path" "$APP_ID"', SCRIPT)
        self.assertIn("Game window is not visible during", SCRIPT)

    def test_primary_touchscreen_swipe_targets_primary_display(self):
        self.assertIn("input touchscreen -d 0 swipe", SCRIPT)

    def test_fallback_swipe_targets_primary_display(self):
        self.assertIn("input -d 0 swipe", SCRIPT)

    def test_render_mode_tap_targets_primary_display(self):
        self.assertIn('input -d 0 tap "$render_x" "$render_y"', SCRIPT)

    def test_preview_launcher_label_identifies_the_3d_diagnostic_build(self):
        manifest = Path(__file__).parents[1] / "app/src/main/AndroidManifest.xml"
        self.assertIn('android:label="Valle de Cernego · Prueba 3D 0.29.1"', manifest.read_text(encoding="utf-8"))

    def test_hud_reports_the_actual_gpu_and_selected_player_shader(self):
        root = Path(__file__).parents[1]
        policy = (root / "app/src/main/java/es/valledecernego/game/PlayerGpuPolicy.java").read_text(encoding="utf-8")
        activity = (root / "app/src/main/java/es/valledecernego/game/MainActivity.java").read_text(encoding="utf-8")
        self.assertIn("diagnosticLabel(String glRenderer, boolean simpleShaderSelected,", policy)
        self.assertIn("BOX60", policy)
        self.assertIn("getPlayerGpuDiagnostic()", activity)
        self.assertIn("PlayerGpuPolicy.diagnosticLabel(playerGpuRenderer,forceCompatibilityPlayerModel,", activity)
        self.assertIn("PLAYER_GPU_PROFILE renderer=", activity)

    def test_release_smoke_uses_a_side_by_side_preview_package(self):
        self.assertIn('APP_ID="es.valledecernego.game.preview"', SCRIPT)
        self.assertIn('ACTIVITY_CLASS="es.valledecernego.game.MainActivity"', SCRIPT)
        self.assertIn('am start -n "$APP_ID/$ACTIVITY_CLASS"', SCRIPT)
        self.assertIn('--ez smoke_test_mode true', SCRIPT)
        self.assertIn('--ez force_box_compatibility true', SCRIPT)

    def test_smoke_scene_uses_the_real_player_renderer_but_skips_full_world_draws(self):
        activity = Path(__file__).parents[1] / "app/src/main/java/es/valledecernego/game/MainActivity.java"
        source = activity.read_text(encoding="utf-8")
        self.assertIn('getBooleanExtra("smoke_test_mode", false)', source)
        self.assertIn('renderer.setSmokeTestMode(smokeTestMode)', source)
        self.assertIn('renderer.setForceBoxCompatibilityForTest(forceBoxCompatibility)', source)
        self.assertIn('forceBoxCompatibilityForTest=enabled && smokeTestMode', source)
        self.assertIn('if(smokeTestMode)', source)
        self.assertIn('drawSmokeTestEnvironment()', source)
        self.assertIn('drawPlayer()', source)
        self.assertIn('SMOKE_SCENE enabled=', source)

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
        self.assertIn("$mode_label movement or camera-follow verification failed after eight fresh gestures.", SCRIPT)
        self.assertIn('exit 1', SCRIPT)

    def test_new_session_defaults_to_detailed_model_and_preserves_choice_across_egl_recreation(self):
        policy = Path(__file__).parents[1] / "app/src/main/java/es/valledecernego/game/PlayerRenderPolicy.java"
        activity = Path(__file__).parents[1] / "app/src/main/java/es/valledecernego/game/MainActivity.java"
        self.assertIn("DEFAULT_COMPATIBILITY_MODE = false", policy.read_text(encoding="utf-8"))
        source = activity.read_text(encoding="utf-8")
        self.assertIn("forceCompatibilityPlayerModel=PlayerRenderPolicy.DEFAULT_COMPATIBILITY_MODE", source)
        self.assertIn("playerModePinned=false", source)
        self.assertIn("PlayerGpuPolicy.shouldPreferSimpleShader(playerGpuRenderer)", source)
        self.assertNotIn("forceCompatibilityPlayerModel=false;", source)

    def test_poco_mali_g615_selects_full_box_only_humanoid(self):
        root = Path(__file__).parents[1]
        policy = (root / "app/src/main/java/es/valledecernego/game/PlayerGpuPolicy.java").read_text(encoding="utf-8")
        activity = (root / "app/src/main/java/es/valledecernego/game/MainActivity.java").read_text(encoding="utf-8")
        self.assertIn("mali-g615", policy)
        self.assertIn("drawPlayerBoxCompatibility()", activity)
        self.assertIn("PlayerGpuPolicy.shouldUseBoxOnlyGeometry(playerGpuRenderer,", activity)
        self.assertIn("forceBoxCompatibilityPlayerModel || forceBoxCompatibilityForTest", activity)
        self.assertIn("return forceCompatibilityPlayerModel", activity)
        self.assertIn("forceCompatibilityPlayerModel\n                && PlayerGpuPolicy.shouldUseBoxOnlyGeometry", activity)
        self.assertIn("boxCompatibilityParts=PlayerModelSpec.boxCompatibilityParts()", activity)

    def test_manual_box60_override_does_not_depend_on_gpu_renderer_name(self):
        root = Path(__file__).parents[1]
        policy = (root / "app/src/main/java/es/valledecernego/game/PlayerGpuPolicy.java").read_text(encoding="utf-8")
        activity = (root / "app/src/main/java/es/valledecernego/game/MainActivity.java").read_text(encoding="utf-8")
        self.assertIn("shouldUseBoxOnlyGeometry(String glRenderer, boolean forceBoxOnly)", policy)
        self.assertIn("forceBoxCompatibilityPlayerModel=forceCompatibilityPlayerModel", activity)
        self.assertIn('return isBoxCompatibilityActive()?"BOX"', activity)
        self.assertIn('PLAYER_MODE user-selected="+selectedMode', activity)

    def test_promoted_fallback_shader_keeps_rounded_humanoid_available(self):
        activity = Path(__file__).parents[1] / "app/src/main/java/es/valledecernego/game/MainActivity.java"
        source = activity.read_text(encoding="utf-8")
        self.assertIn("PlayerShaderPlan.resolve(", source)
        self.assertIn("silhouetteReady=shaderPlan.silhouetteReady;", source)
        self.assertIn("PLAYER_SHADER_PLAN primary=", source)

    def test_pixel_verifier_requires_both_arms_and_both_legs_not_only_a_blue_torso(self):
        verifier = Path(__file__).with_name("verify-player-screenshot.py").read_text(encoding="utf-8")
        for limb in ("left_arm", "right_arm", "left_leg", "right_leg"):
            self.assertIn('"{}": 80'.format(limb), verifier)
        self.assertIn("A single blue rectangle can satisfy the old torso-colour check", verifier)

    def test_box60_smoke_requires_the_textured_60_part_material_path(self):
        self.assertIn('PLAYER_BOX_MATERIALS frame=textured parts=60', SCRIPT)
        self.assertIn('manual_box_texture_line', SCRIPT)
        self.assertIn('box_texture_line', SCRIPT)

    def test_smoke_captures_and_moves_detailed_default_then_manual_box60_model(self):
        self.assertIn("PLAYER_STATE .*PARTS=60 FALLBACK=0", SCRIPT)
        self.assertIn('valle-cernego-detailed.png', SCRIPT)
        self.assertIn('valle-cernego-detailed-moving.png', SCRIPT)
        self.assertIn('PLAYER_MODE user-selected=box-only-shader-60', SCRIPT)
        self.assertIn('PLAYER_COMPATIBILITY geometry=box-only-60 GL=0x0', SCRIPT)
        self.assertIn('valle-cernego-box60-manual.png', SCRIPT)
        self.assertIn('valle-cernego-box60-manual-moving.png', SCRIPT)
        self.assertIn('PLAYER_MODE context-created=box-only-shader-60', SCRIPT)
        self.assertIn('valle-cernego-box60.png', SCRIPT)
        self.assertIn('valle-cernego-box60-moving.png', SCRIPT)
        self.assertIn('PLAYER_STATE .*PARTS=60 FALLBACK=60', SCRIPT)
        self.assertIn('verify-player-motion.py "$initial_telemetry" "$moving_telemetry"', SCRIPT)

    def test_smoke_waits_for_systemui_recovery_before_tapping_render_mode(self):
        wait = SCRIPT.index('wait_for_clear_windows "before manual BOX60 mode toggle"')
        tap = SCRIPT.index('adb shell input -d 0 tap "$render_x" "$render_y"')
        self.assertLess(wait, tap)

    def test_systemui_anr_recovery_uses_only_a_verified_close_point(self):
        self.assertIn('--dismiss-point "$dump_path" "$screen_w" "$screen_h"', SCRIPT)
        self.assertIn('adb shell input tap "$close_x" "$close_y"', SCRIPT)

    def test_cpu_only_emulator_uses_a_longer_swipe_without_lowering_motion_floor(self):
        self.assertIn('swipe "$joy_x" "$joy_y" "$move_x" "$joy_y" 4000', SCRIPT)
        motion_script = Path(__file__).with_name("verify-player-motion.py").read_text(encoding="utf-8")
        self.assertIn("MIN_MOVEMENT_METERS = 0.25", motion_script)


if __name__ == "__main__":
    unittest.main()
