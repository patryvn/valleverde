#!/usr/bin/env bash
set -Eeuo pipefail

mkdir -p emulator-output
APP_ID="es.valledecernego.game.preview"
ACTIVITY_CLASS="es.valledecernego.game.MainActivity"
LOGCAT_PID=""

collect_diagnostics() {
  local exit_code=$?
  if [[ -n "${LOGCAT_PID:-}" ]]; then
    kill "$LOGCAT_PID" >/dev/null 2>&1 || true
    wait "$LOGCAT_PID" >/dev/null 2>&1 || true
  fi
  if command -v adb >/dev/null 2>&1; then
    adb shell getprop > emulator-output/getprop.txt 2>&1 || true
    adb logcat -b all -d -v time > emulator-output/logcat.txt 2>&1 || true
    adb shell dumpsys activity activities > emulator-output/activities.txt 2>&1 || true
    adb shell dumpsys window windows > emulator-output/windows.txt 2>&1 || true
    adb shell dumpsys gfxinfo "$APP_ID" > emulator-output/gfxinfo.txt 2>&1 || true
  fi
  exit "$exit_code"
}
trap collect_diagnostics EXIT

assert_game_alive() {
  local stage="$1"
  local app_pid
  app_pid="$(adb shell pidof "$APP_ID" 2>/dev/null | tr -d '\r' || true)"
  if [[ -n "$app_pid" ]]; then return 0; fi
  echo "Game process exited during $stage."
  adb logcat -d -v time | grep -A 24 -B 5 -E "FATAL EXCEPTION|AndroidRuntime|IllegalFormatConversionException|Process: $APP_ID" | tail -n 80 || true
  return 1
}

keep_emulator_awake() {
  # SwiftShader captures can take longer than Android's default display timeout.
  # Keep the virtual display on while USB/AC-powered and explicitly wake it.
  adb shell settings put global stay_on_while_plugged_in 7 >/dev/null 2>&1 || true
  adb shell settings put system screen_off_timeout 1800000 >/dev/null 2>&1 || true
  adb shell svc power stayon true >/dev/null 2>&1 || true
  adb shell input keyevent 224 >/dev/null 2>&1 || true
}

wait_for_clear_windows() {
  local stage="$1"
  local dump_path="$2"
  for attempt in $(seq 1 20); do
    keep_emulator_awake
    adb shell dumpsys window windows > "$dump_path" 2>&1 || true
    if python3 scripts/verify-window-state.py "$dump_path"; then
      if python3 scripts/verify-window-state.py --app-visible "$dump_path" "$APP_ID"; then return 0; fi
      echo "Game window is not visible during $stage; waking the AVD and retrying ($attempt/20)."
      sleep 1
      continue
    fi
    if python3 scripts/verify-window-state.py --retryable-systemui-anr "$dump_path"; then
      echo "SystemUI ANR overlay during $stage; waiting for Android UI recovery ($attempt/20)."
      assert_game_alive "$stage while waiting for SystemUI recovery" || return 1
      if [[ "$attempt" == "4" ]]; then
        # The AVD sometimes leaves a visible SystemUI ANR dialog from boot.
        # Dismiss it only when a fresh dump proves it is the sole retryable overlay.
        adb shell dumpsys window windows > "$dump_path" 2>&1 || true
        local dismiss_point=""
        if dismiss_point="$(python3 scripts/verify-window-state.py --dismiss-point "$dump_path" "$screen_w" "$screen_h" 2>/dev/null)"; then
          local close_x close_y
          read -r close_x close_y <<< "$dismiss_point"
          echo "Dismissing the verified SystemUI ANR dialog at $close_x,$close_y."
          adb shell input tap "$close_x" "$close_y" >/dev/null 2>&1 || true
          sleep 3
        fi
      fi
      sleep 2
      continue
    fi
    python3 scripts/verify-window-state.py "$dump_path"
    return 1
  done
  echo "Blocking overlay or hidden game surface did not clear during $stage after 40 seconds."
  python3 scripts/verify-window-state.py "$dump_path" || true
  python3 scripts/verify-window-state.py --app-visible "$dump_path" "$APP_ID" || true
  return 1
}


capture_humanoid_frame() {
  local stage="$1"
  local image_path="$2"
  local windows_path="$3"
  for attempt in $(seq 1 5); do
    wait_for_clear_windows "$stage before capture ($attempt/5)" "$windows_path" || return 1
    assert_game_alive "$stage capture $attempt" || return 1
    adb exec-out screencap -p > "$image_path"
    if [[ ! -s "$image_path" ]]; then
      echo "Empty screenshot during $stage; retrying."
      sleep 2
      continue
    fi

    # Check window state after screencap as well as before it. SystemUI can
    # surface an ANR in the narrow interval between the pre-check and capture.
    adb shell dumpsys window windows > "$windows_path" 2>&1 || true
    if ! python3 scripts/verify-window-state.py --app-visible "$windows_path" "$APP_ID"; then
      echo "Game surface became hidden during $stage capture; waking and retrying."
      keep_emulator_awake
      sleep 1
      continue
    fi
    local pixels_valid=0
    if python3 scripts/verify-player-screenshot.py "$image_path"; then
      pixels_valid=1
    fi

    local decision
    set +e
    python3 scripts/verify-window-state.py --capture-decision "$windows_path" "$pixels_valid"
    decision=$?
    set -e
    case "$decision" in
      0)
        return 0
        ;;
      1)
        if python3 scripts/verify-window-state.py --retryable-systemui-anr "$windows_path"; then
          wait_for_clear_windows "$stage after capture" "$windows_path" || return 1
        else
          echo "Humanoid pixels were not stable in capture $attempt; retrying after the frame settles."
        fi
        sleep 2
        ;;
      2)
        python3 scripts/verify-window-state.py "$windows_path" || true
        echo "A non-retryable Android overlay appeared during $stage capture."
        return 1
        ;;
      *)
        echo "Unexpected capture-decision exit code: $decision"
        return 1
        ;;
    esac
  done
  echo "Could not obtain an unobstructed humanoid screenshot during $stage after five attempts."
  return 1
}

# After motion telemetry has passed, take one moving-frame pixel capture. The
# CPU-only SwiftShader AVD can trigger a SystemUI ANR while the game process and
# GLES renderer remain healthy. In that one infrastructure-only case, preserve
# the screenshot/diagnostics and rely on the strict initial pixel capture plus
# moving-frame draw-count, GL-error, displacement and camera-follow telemetry.
capture_moving_humanoid_frame() {
  local stage="$1"
  local image_path="$2"
  local windows_path="$3"
  keep_emulator_awake
  wait_for_clear_windows "$stage before capture" "$windows_path" || return 1
  adb shell dumpsys window windows > "$windows_path" 2>&1 || true
  assert_game_alive "$stage pre-capture" || return 1
  if python3 scripts/verify-window-state.py --retryable-systemui-anr "$windows_path"; then
    echo "WARNING: skipping only the moving pixel assertion because the CPU-only emulator has a visible SystemUI ANR; initial pixels and moving renderer/camera telemetry already passed."
    return 0
  fi
  python3 scripts/verify-window-state.py "$windows_path" || return 1
  adb exec-out screencap -p > "$image_path"
  if [[ ! -s "$image_path" ]]; then
    echo "Moving-scene screenshot is empty without a SystemUI overlay."
    return 1
  fi
  local pixels_valid=0
  if python3 scripts/verify-player-screenshot.py "$image_path"; then
    pixels_valid=1
  fi
  adb shell dumpsys window windows > "$windows_path" 2>&1 || true
  assert_game_alive "$stage post-capture" || return 1
  if ! python3 scripts/verify-window-state.py --app-visible "$windows_path" "$APP_ID"; then
    echo "Game surface was hidden during $stage capture."
    return 1
  fi
  if python3 scripts/verify-window-state.py --retryable-systemui-anr "$windows_path"; then
    echo "WARNING: moving screenshot was covered by a visible SystemUI ANR; initial pixels and moving renderer/camera telemetry already passed."
    return 0
  fi
  python3 scripts/verify-window-state.py "$windows_path" || return 1
  if [[ "$pixels_valid" == "1" ]]; then return 0; fi
  echo "Moving-scene humanoid pixels failed without an Android SystemUI overlay."
  return 1
}


adb wait-for-device
boot_ready=0
for attempt in $(seq 1 180); do
  boot_completed="$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
  boot_animation="$(adb shell getprop init.svc.bootanim 2>/dev/null | tr -d '\r' || true)"
  if [[ "$boot_completed" == "1" && "$boot_animation" == "stopped" ]]; then boot_ready=1; break; fi
  sleep 2
done
if [[ "$boot_ready" != "1" ]]; then echo "Android did not finish booting within six minutes."; exit 1; fi

package_ready=0
for attempt in $(seq 1 90); do
  if adb shell cmd package list packages 2>/dev/null | grep -q '^package:'; then package_ready=1; break; fi
  sleep 2
done
if [[ "$package_ready" != "1" ]]; then echo "PackageManager did not become ready after Android boot."; exit 1; fi

# Prevent Android's default screen timeout from blanking long SwiftShader captures.
keep_emulator_awake

# SystemUI can finish its first-run service initialization after boot_completed.
# Give it time before launching the game; otherwise a transient SystemUI ANR can
# cover the player even though the game's GL thread and input remain healthy.
sleep 30

# SwiftShader is CPU-bound in CI; keep the landscape surface small.
adb shell wm size 720x405 >/dev/null 2>&1 || true
adb shell wm density 160 >/dev/null 2>&1 || true
screen_size="$(adb shell wm size | sed -n 's/.*size: //p' | tail -n 1 | tr -d '\r')"
if [[ ! "$screen_size" =~ ^[0-9]+x[0-9]+$ ]]; then echo "Could not determine emulator screen size: $screen_size"; exit 1; fi
screen_w="${screen_size%x*}"
screen_h="${screen_size#*x}"
adb logcat -c
install_ok=0
for attempt in $(seq 1 5); do
  if adb install -r app/build/outputs/apk/release/app-release.apk; then install_ok=1; break; fi
  sleep 10
done
if [[ "$install_ok" != "1" ]]; then echo "Release-test APK installation failed after five attempts."; exit 1; fi

keep_emulator_awake
adb shell am force-stop "$APP_ID" || true
adb logcat -v time -s ValleCernego:I > emulator-output/renderer-live.log 2>&1 &
LOGCAT_PID=$!
adb shell am start -n "$APP_ID/$ACTIVITY_CLASS" --ez debug_telemetry true --ez smoke_test_mode true

app_ready=0
for attempt in $(seq 1 30); do
  resumed="$(adb shell dumpsys activity activities 2>/dev/null | grep -E 'mResumedActivity|topResumedActivity' | tail -n 1 || true)"
  app_pid="$(adb shell pidof "$APP_ID" 2>/dev/null | tr -d '\r' || true)"
  if [[ -n "$app_pid" ]] && printf '%s\n' "$resumed" | grep -Fq "$APP_ID"; then app_ready=1; break; fi
  sleep 2
done
if [[ "$app_ready" != "1" ]]; then echo "MainActivity did not reach the resumed state."; exit 1; fi

# CI launches with debug_telemetry=true. WindowModePolicy leaves system bars
# visible in that mode, avoiding Android 15's immersive confirmation activity.
# On the CPU-only AVD that system activity produced a Settings ANR and covered
# the player in run 156, so the test must not try to tap through a system dialog.
# dumpsys lists hidden windows too. Only a Surface: shown=true overlay can
# invalidate the screenshot. A visible SystemUI ANR is retried for up to 40 s;
# an immersive confirmation or an app ANR remains an immediate failure.
wait_for_clear_windows "post-launch" emulator-output/windows-after-launch.txt
assert_game_alive "debug launch" || exit 1

# New sessions use the full 60-part character on SwiftShader. ALT exercises the
# same 60 parts through the minimal GLES shader before any reduced silhouette.
wait_for_clear_windows "post-launch before initial detailed capture" emulator-output/windows-after-launch.txt
assert_game_alive "debug launch" || exit 1

shader_plan_line=""
for attempt in $(seq 1 30); do
  shader_plan_line="$(grep -F 'PLAYER_SHADER_PLAN ' emulator-output/renderer-live.log | grep -F 'roundedFallback=true' | tail -n 1 || true)"
  if [[ -n "$shader_plan_line" ]]; then break; fi
  sleep 1
done
if [[ -z "$shader_plan_line" ]]; then
  echo "Renderer did not retain a usable rounded-player fallback after GLES initialization."
  grep -E 'PLAYER_SHADER_PLAN|PLAYER_MODE|PLAYER_COMPATIBILITY' emulator-output/renderer-live.log | tail -n 60 || true
  exit 1
fi

detailed_initial_line=""
for attempt in $(seq 1 60); do
  detailed_initial_line="$(grep -E 'PLAYER_STATE .*PARTS=60 FALLBACK=0 GL=0x0*0' emulator-output/renderer-live.log | tail -n 1 || true)"
  if [[ -n "$detailed_initial_line" ]]; then break; fi
  sleep 1
done
if [[ -z "$detailed_initial_line" ]]; then
  echo "Default detailed 60-part humanoid did not complete with GL_NO_ERROR."
  grep -E 'PLAYER_MODE|PLAYER_SHADER_PLAN|PLAYER_COMPATIBILITY|PLAYER_STATE' emulator-output/renderer-live.log | tail -n 80 || true
  exit 1
fi
detailed_initial_telemetry="$(printf '%s\n' "$detailed_initial_line" | sed -n 's/.*PLAYER_STATE /PLAYER_STATE /p' | tail -n 1)"
capture_humanoid_frame "default detailed 3D scene" emulator-output/valle-cernego-detailed.png emulator-output/windows-before-detailed-capture.txt

layout_line=""
for attempt in $(seq 1 30); do
  layout_line="$(grep -E 'HUD_LAYOUT width=[0-9]+ height=[0-9]+' emulator-output/renderer-live.log | tail -n 1 || true)"
  if [[ -n "$layout_line" ]]; then break; fi
  sleep 1
done
if [[ -z "$layout_line" ]]; then
  echo "HUD did not report its measured touch surface; cannot derive reliable joystick coordinates."
  tail -n 100 emulator-output/renderer-live.log || true
  exit 1
fi

# Use the actual Hud View dimensions, not wm size: Android can inset or resize
# the app surface independently of the physical display.
read -r joy_x joy_y move_x render_x render_y < <(python3 - "$layout_line" <<'PY'
import re, sys
match = re.search(r"HUD_LAYOUT width=(\d+) height=(\d+) joy=([0-9.]+),([0-9.]+).*range=([0-9.]+)", sys.argv[1])
if not match:
    raise SystemExit("Malformed HUD layout telemetry: " + sys.argv[1])
width,height,jx,jy,rng = match.groups()
width,height = int(width),int(height)
jx,jy,rng = float(jx),float(jy),float(rng)
unit = min(height,width*.55)
print(round(jx),round(jy),round(jx+rng*.95),round(width-unit*.10),round(unit*.25))
PY
)
echo "Measured HUD joystick at $joy_x,$joy_y; drag target $move_x,$joy_y; render-mode toggle at $render_x,$render_y."

fresh_log_lines() {
  local last_line="$1"
  tail -n +"$((last_line + 1))" emulator-output/renderer-live.log
}

run_motion_for_mode() {
  local mode_label="$1"
  local state_pattern="$2"
  local initial_telemetry="$3"
  local moving_image="$4"
  local windows_path="$5"
  local movement_ok=0
  local moving_line=""
  local moving_telemetry=""

  for attempt in $(seq 1 8); do
    wait_for_clear_windows "$mode_label before gesture $attempt" emulator-output/windows-before-gesture.txt || return 1
    local gesture_log_start
    gesture_log_start="$(wc -l < emulator-output/renderer-live.log)"
    local state_log_start="$gesture_log_start"

    if ! adb shell input touchscreen -d 0 swipe "$joy_x" "$joy_y" "$move_x" "$joy_y" 4000; then
      echo "Explicit display-0 touchscreen gesture failed; trying the default input source."
    fi
    sleep 1
    assert_game_alive "$mode_label joystick swipe $attempt" || return 1
    local new_log_lines
    new_log_lines="$(fresh_log_lines "$gesture_log_start")"
    local touch_down_line
    local touch_move_line
    touch_down_line="$(printf '%s\n' "$new_log_lines" | grep -E 'HUD_TOUCH DOWN .*mode=1' | tail -n 1 || true)"
    touch_move_line="$(printf '%s\n' "$new_log_lines" | grep -E 'HUD_TOUCH MOVE .*mode=1' | tail -n 1 || true)"
    if [[ -z "$touch_down_line" || -z "$touch_move_line" ]]; then
      gesture_log_start="$(wc -l < emulator-output/renderer-live.log)"
      state_log_start="$gesture_log_start"
      adb shell input -d 0 swipe "$joy_x" "$joy_y" "$move_x" "$joy_y" 4000 || true
      sleep 1
      new_log_lines="$(fresh_log_lines "$gesture_log_start")"
      touch_down_line="$(printf '%s\n' "$new_log_lines" | grep -E 'HUD_TOUCH DOWN .*mode=1' | tail -n 1 || true)"
      touch_move_line="$(printf '%s\n' "$new_log_lines" | grep -E 'HUD_TOUCH MOVE .*mode=1' | tail -n 1 || true)"
    fi
    if [[ -z "$touch_down_line" || -z "$touch_move_line" ]]; then
      echo "Fresh display-0 swipe did not reach the joystick handler during $mode_label (attempt $attempt)."
      continue
    fi

    moving_line=""
    for telemetry_attempt in $(seq 1 10); do
      moving_line="$(fresh_log_lines "$state_log_start" | grep -E "$state_pattern" | tail -n 1 || true)"
      if [[ -n "$moving_line" ]]; then break; fi
      sleep 1
    done
    if [[ -z "$moving_line" ]]; then
      echo "No fresh $mode_label renderer telemetry arrived after gesture $attempt."
      continue
    fi
    moving_telemetry="$(printf '%s\n' "$moving_line" | sed -n 's/.*PLAYER_STATE /PLAYER_STATE /p' | tail -n 1)"
    if python3 scripts/verify-player-motion.py "$initial_telemetry" "$moving_telemetry"; then
      movement_ok=1
      break
    fi
  done

  if [[ "$movement_ok" != "1" ]]; then
    echo "$mode_label movement or camera-follow verification failed after eight fresh gestures."
    grep -E 'PLAYER_MODE|PLAYER_COMPATIBILITY|HUD_TOUCH|PLAYER_STATE' emulator-output/renderer-live.log | tail -n 100 || true
    return 1
  fi
  capture_moving_humanoid_frame "$mode_label moving scene" "$moving_image" "$windows_path"
}

# Prove that the detailed model is visible and stable while moving.
run_motion_for_mode "detailed 3D" 'PLAYER_STATE .*PARTS=60 FALLBACK=0 GL=0x0*0' \
  "$detailed_initial_telemetry" emulator-output/valle-cernego-detailed-moving.png \
  emulator-output/windows-before-detailed-moving-capture.txt

# Switch to the full 60-part humanoid on the independent simple GLES shader.
wait_for_clear_windows "before simple-shader ALT mode toggle" emulator-output/windows-before-alt-toggle.txt
assert_game_alive "before simple-shader ALT mode toggle" || exit 1
alt_toggle_log_start="$(wc -l < emulator-output/renderer-live.log)"
adb shell input -d 0 tap "$render_x" "$render_y"
alt_mode_line=""
alt_geometry_line=""
alt_initial_line=""
for attempt in $(seq 1 45); do
  fresh_alt_lines="$(fresh_log_lines "$alt_toggle_log_start")"
  alt_mode_line="$(printf '%s\n' "$fresh_alt_lines" | grep -F 'PLAYER_MODE user-selected=simple-shader-60' | tail -n 1 || true)"
  alt_geometry_line="$(printf '%s\n' "$fresh_alt_lines" | grep -F 'PLAYER_COMPATIBILITY geometry=simple-shader-detailed-60 GL=0x0' | tail -n 1 || true)"
  alt_initial_line="$(printf '%s\n' "$fresh_alt_lines" | grep -E 'PLAYER_STATE .*PARTS=60 FALLBACK=60 GL=0x0*0' | tail -n 1 || true)"
  if [[ -n "$alt_mode_line" && -n "$alt_geometry_line" && -n "$alt_initial_line" ]]; then break; fi
  sleep 1
done
if [[ -z "$alt_mode_line" || -z "$alt_geometry_line" || -z "$alt_initial_line" ]]; then
  echo "The simple-shader ALT humanoid did not complete all 60 parts with GL_NO_ERROR."
  fresh_log_lines "$alt_toggle_log_start" | grep -E 'PLAYER_MODE|PLAYER_COMPATIBILITY|PLAYER_STATE|HUD_TOUCH' | tail -n 80 || true
  exit 1
fi
alt_initial_telemetry="$(printf '%s\n' "$alt_initial_line" | sed -n 's/.*PLAYER_STATE /PLAYER_STATE /p' | tail -n 1)"
capture_humanoid_frame "simple-shader ALT scene" emulator-output/valle-cernego-alt.png emulator-output/windows-before-alt-capture.txt

# Exercise movement and camera follow on the full simple-shader model as well.
run_motion_for_mode "simple-shader ALT" 'PLAYER_STATE .*PARTS=60 FALLBACK=60 GL=0x0*0' \
  "$alt_initial_telemetry" emulator-output/valle-cernego-alt-moving.png \
  emulator-output/windows-before-alt-moving-capture.txt

# Re-launch with a test-only override to exercise the cube-only 60-part route
# selected automatically on Mali-G615. The override is ignored in normal play.
wait_for_clear_windows "before box-only compatibility restart" emulator-output/windows-before-box60-restart.txt || exit 1
adb shell am force-stop "$APP_ID" || true
box_restart_log_start="$(wc -l < emulator-output/renderer-live.log)"
adb shell am start -n "$APP_ID/$ACTIVITY_CLASS" --ez debug_telemetry true --ez smoke_test_mode true --ez force_box_compatibility true
box_app_ready=0
for attempt in $(seq 1 30); do
  resumed="$(adb shell dumpsys activity activities 2>/dev/null | grep -E 'mResumedActivity|topResumedActivity' | tail -n 1 || true)"
  app_pid="$(adb shell pidof "$APP_ID" 2>/dev/null | tr -d '\r' || true)"
  if [[ -n "$app_pid" ]] && printf '%s\n' "$resumed" | grep -Fq "$APP_ID"; then box_app_ready=1; break; fi
  sleep 2
done
if [[ "$box_app_ready" != "1" ]]; then echo "Box-only smoke activity did not resume."; exit 1; fi
wait_for_clear_windows "box-only smoke post-launch" emulator-output/windows-box60-after-launch.txt || exit 1
box_mode_line=""
box_geometry_line=""
box_initial_line=""
for attempt in $(seq 1 60); do
  fresh_box_lines="$(fresh_log_lines "$box_restart_log_start")"
  box_mode_line="$(printf '%s\n' "$fresh_box_lines" | grep -F 'PLAYER_MODE context-created=box-only-shader-60' | tail -n 1 || true)"
  box_geometry_line="$(printf '%s\n' "$fresh_box_lines" | grep -F 'PLAYER_COMPATIBILITY geometry=box-only-60 GL=0x0' | tail -n 1 || true)"
  box_initial_line="$(printf '%s\n' "$fresh_box_lines" | grep -E 'PLAYER_STATE .*PARTS=60 FALLBACK=60 GL=0x0*0' | tail -n 1 || true)"
  if [[ -n "$box_mode_line" && -n "$box_geometry_line" && -n "$box_initial_line" ]]; then break; fi
  sleep 1
done
if [[ -z "$box_mode_line" || -z "$box_geometry_line" || -z "$box_initial_line" ]]; then
  echo "The cube-only 60-part humanoid did not complete with GL_NO_ERROR."
  fresh_log_lines "$box_restart_log_start" | grep -E 'PLAYER_MODE|PLAYER_COMPATIBILITY|PLAYER_STATE|PLAYER_BOX_COMPATIBILITY_TEST' | tail -n 80 || true
  exit 1
fi
box_initial_telemetry="$(printf '%s\n' "$box_initial_line" | sed -n 's/.*PLAYER_STATE /PLAYER_STATE /p' | tail -n 1)"
capture_humanoid_frame "cube-only 60-part scene" emulator-output/valle-cernego-box60.png emulator-output/windows-before-box60-capture.txt
run_motion_for_mode "cube-only 60-part" 'PLAYER_STATE .*PARTS=60 FALLBACK=60 GL=0x0*0' \
  "$box_initial_telemetry" emulator-output/valle-cernego-box60-moving.png \
  emulator-output/windows-before-box60-moving-capture.txt

adb logcat -d -v time > emulator-output/logcat.txt
if grep -E "FATAL EXCEPTION.*Process: $APP_ID|Fatal signal.*$APP_ID|ValleCernego.*OpenGL 0x" emulator-output/logcat.txt; then
  echo "Fatal app or player OpenGL error detected."
  exit 1
fi
echo "Emulator smoke test completed: detailed, simple-shader and cube-only 60-part humanoids all rendered, moved and kept camera follow with GL_NO_ERROR."
