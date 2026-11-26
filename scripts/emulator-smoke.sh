#!/usr/bin/env bash
set -Eeuo pipefail

mkdir -p emulator-output
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
    adb shell dumpsys gfxinfo es.valledecernego.game > emulator-output/gfxinfo.txt 2>&1 || true
  fi
  exit "$exit_code"
}
trap collect_diagnostics EXIT

assert_game_alive() {
  local stage="$1"
  local app_pid
  app_pid="$(adb shell pidof es.valledecernego.game 2>/dev/null | tr -d '\r' || true)"
  if [[ -n "$app_pid" ]]; then return 0; fi
  echo "Game process exited during $stage."
  adb logcat -d -v time | grep -A 24 -B 5 -E 'FATAL EXCEPTION|AndroidRuntime|IllegalFormatConversionException|Process: es.valledecernego.game' | tail -n 80 || true
  return 1
}

wait_for_clear_windows() {
  local stage="$1"
  local dump_path="$2"
  for attempt in $(seq 1 20); do
    adb shell dumpsys window windows > "$dump_path" 2>&1 || true
    if python3 scripts/verify-window-state.py "$dump_path"; then return 0; fi
    if python3 scripts/verify-window-state.py --retryable-systemui-anr "$dump_path"; then
      echo "SystemUI ANR overlay during $stage; waiting for Android UI recovery ($attempt/20)."
      assert_game_alive "$stage while waiting for SystemUI recovery" || return 1
      if [[ "$attempt" == "4" ]]; then
        adb shell input keyevent 4 >/dev/null 2>&1 || true
      fi
      sleep 2
      continue
    fi
    python3 scripts/verify-window-state.py "$dump_path"
    return 1
  done
  echo "SystemUI ANR overlay did not clear during $stage after 40 seconds."
  python3 scripts/verify-window-state.py "$dump_path"
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

adb shell input keyevent 224 || true
adb shell am force-stop es.valledecernego.game || true
adb logcat -v time -s ValleCernego:I > emulator-output/renderer-live.log 2>&1 &
LOGCAT_PID=$!
adb shell am start -n es.valledecernego.game/.MainActivity --ez debug_telemetry true

app_ready=0
for attempt in $(seq 1 30); do
  resumed="$(adb shell dumpsys activity activities 2>/dev/null | grep -E 'mResumedActivity|topResumedActivity' | tail -n 1 || true)"
  app_pid="$(adb shell pidof es.valledecernego.game 2>/dev/null | tr -d '\r' || true)"
  if [[ -n "$app_pid" ]] && printf '%s\n' "$resumed" | grep -Fq 'es.valledecernego.game'; then app_ready=1; break; fi
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

# Logcat telemetry replaces repeated UiAutomator dumps on the CPU-only AVD.
initial_line=""
for attempt in $(seq 1 60); do
  initial_line="$(grep -E 'PLAYER_STATE .*PARTS=(60|11) FALLBACK=(0|11) GL=0x0*0' emulator-output/renderer-live.log | tail -n 1 || true)"
  if [[ -n "$initial_line" ]]; then break; fi
  sleep 1
done
if [[ -z "$initial_line" ]]; then
  echo "Renderer did not report the detailed model or the 11-part humanoid fallback with GL_NO_ERROR."
  tail -n 80 emulator-output/renderer-live.log || true
  exit 1
fi
initial_telemetry="$(printf '%s\n' "$initial_line" | sed -n 's/.*PLAYER_STATE /PLAYER_STATE /p' | tail -n 1)"
assert_game_alive "initial scene capture" || exit 1
wait_for_clear_windows "initial capture" emulator-output/windows-before-initial-capture.txt
adb exec-out screencap -p > emulator-output/valle-cernego-initial.png
test -s emulator-output/valle-cernego-initial.png
python3 scripts/verify-player-screenshot.py emulator-output/valle-cernego-initial.png

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
read -r joy_x joy_y move_x < <(python3 - "$layout_line" <<'PY'
import re, sys
match = re.search(r"HUD_LAYOUT width=(\d+) height=(\d+) joy=([0-9.]+),([0-9.]+).*range=([0-9.]+)", sys.argv[1])
if not match:
    raise SystemExit("Malformed HUD layout telemetry: " + sys.argv[1])
width,height,jx,jy,rng = match.groups()
jx,jy,rng = float(jx),float(jy),float(rng)
print(round(jx),round(jy),round(jx+rng*.95))
PY
)
echo "Measured HUD joystick at $joy_x,$joy_y; drag target $move_x,$joy_y."

movement_ok=0
moving_line=""
moving_telemetry=""
for attempt in $(seq 1 8); do
  # Explicit touchscreen source avoids depending on the shell input default.
  adb shell input touchscreen swipe "$joy_x" "$joy_y" "$move_x" "$joy_y" 2200
  sleep 1
  assert_game_alive "joystick swipe $attempt" || exit 1
  touch_down_line="$(grep -E 'HUD_TOUCH DOWN .*mode=1' emulator-output/renderer-live.log | tail -n 1 || true)"
  touch_move_line="$(grep -E 'HUD_TOUCH MOVE .*mode=1' emulator-output/renderer-live.log | tail -n 1 || true)"
  if [[ -z "$touch_down_line" || -z "$touch_move_line" ]]; then
    echo "Touchscreen swipe did not reach the joystick handler (down=\${touch_down_line:+yes}, move=\${touch_move_line:+yes})."
    continue
  fi
  moving_line="$(grep -E 'PLAYER_STATE .*PARTS=(60|11) FALLBACK=(0|11) GL=0x0*0' emulator-output/renderer-live.log | tail -n 1 || true)"
  if [[ -z "$moving_line" ]]; then continue; fi
  moving_telemetry="$(printf '%s\n' "$moving_line" | sed -n 's/.*PLAYER_STATE /PLAYER_STATE /p' | tail -n 1)"
  if python3 scripts/verify-player-motion.py "$initial_telemetry" "$moving_telemetry"; then
    movement_ok=1
    break
  fi
done
if [[ "$movement_ok" != "1" ]]; then
  echo "Player movement or camera follow verification failed after eight measured touchscreen gestures."
  grep -E 'HUD_LAYOUT|HUD_TOUCH|PLAYER_STATE' emulator-output/renderer-live.log | tail -n 100 || true
  exit 1
fi
wait_for_clear_windows "moving capture" emulator-output/windows-before-moving-capture.txt
adb exec-out screencap -p > emulator-output/valle-cernego-moving.png
test -s emulator-output/valle-cernego-moving.png
python3 scripts/verify-player-screenshot.py emulator-output/valle-cernego-moving.png

adb logcat -d -v time > emulator-output/logcat.txt
if grep -E "FATAL EXCEPTION.*Process: es.valledecernego.game|Fatal signal.*es.valledecernego.game|ValleCernego.*OpenGL 0x" emulator-output/logcat.txt; then
  echo "Fatal app or player OpenGL error detected."
  exit 1
fi
echo "Emulator smoke test completed: 60 player parts, GL_NO_ERROR, joystick movement and camera follow verified."
