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

# SwiftShader is CPU-bound in CI; keep the landscape surface small.
adb shell wm size 960x540 >/dev/null 2>&1 || true
adb shell wm density 160 >/dev/null 2>&1 || true
screen_size="$(adb shell wm size | sed -n 's/.*size: //p' | tail -n 1 | tr -d '\r')"
if [[ ! "$screen_size" =~ ^[0-9]+x[0-9]+$ ]]; then echo "Could not determine emulator screen size: $screen_size"; exit 1; fi
screen_w="${screen_size%x*}"
screen_h="${screen_size#*x}"
adb logcat -c
install_ok=0
for attempt in $(seq 1 5); do
  if adb install -r app/build/outputs/apk/debug/app-debug.apk; then install_ok=1; break; fi
  sleep 10
done
if [[ "$install_ok" != "1" ]]; then echo "APK installation failed after five attempts."; exit 1; fi

adb shell input keyevent 224 || true
adb shell input keyevent 82 || true
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

# Android 15 can place an immersive-mode confirmation window above the game on
# the first launch. It keeps the Activity resumed but intercepts every touch,
# which made the joystick test report zero movement. Dismiss its "Got it" button
# before capturing pixels or injecting gameplay input.
for attempt in $(seq 1 10); do
  window_dump="$(adb shell dumpsys window windows 2>/dev/null || true)"
  if ! printf '%s\n' "$window_dump" | grep -q 'ImmersiveModeConfirmation'; then break; fi
  adb shell input tap "$((screen_w * 70 / 100))" "$((screen_h * 35 / 100))"
  sleep 1
done
window_dump="$(adb shell dumpsys window windows 2>/dev/null || true)"
if printf '%s\n' "$window_dump" | grep -q 'ImmersiveModeConfirmation'; then
  echo "Android immersive-mode confirmation still covers the game after dismissal attempts."
  printf '%s\n' "$window_dump" | grep -C 2 'ImmersiveModeConfirmation' || true
  exit 1
fi

# Logcat telemetry replaces repeated UiAutomator dumps on the CPU-only AVD.
initial_line=""
for attempt in $(seq 1 60); do
  initial_line="$(grep -E 'PLAYER_STATE .*PARTS=60 GL=0x0*0' emulator-output/renderer-live.log | tail -n 1 || true)"
  if [[ -n "$initial_line" ]]; then break; fi
  sleep 1
done
if [[ -z "$initial_line" ]]; then
  echo "Renderer did not report 60 submitted player parts with GL_NO_ERROR."
  tail -n 80 emulator-output/renderer-live.log || true
  exit 1
fi
initial_telemetry="$(printf '%s\n' "$initial_line" | sed -n 's/.*PLAYER_STATE /PLAYER_STATE /p' | tail -n 1)"
adb exec-out screencap -p > emulator-output/valle-cernego-initial.png
test -s emulator-output/valle-cernego-initial.png

unit=$((screen_h < screen_w * 55 / 100 ? screen_h : screen_w * 55 / 100))
joy_x=$((unit * 20 / 100))
joy_y=$((screen_h - unit * 22 / 100))
move_x=$((joy_x + unit * 10 / 100))
adb shell input swipe "$joy_x" "$joy_y" "$move_x" "$joy_y" 1200 0
sleep 5

moving_line="$(grep -E 'PLAYER_STATE .*PARTS=60 GL=0x0*0' emulator-output/renderer-live.log | tail -n 1 || true)"
if [[ -z "$moving_line" ]]; then
  echo "No valid player telemetry was recorded after the joystick gesture."
  tail -n 80 emulator-output/renderer-live.log || true
  exit 1
fi
moving_telemetry="$(printf '%s\n' "$moving_line" | sed -n 's/.*PLAYER_STATE /PLAYER_STATE /p' | tail -n 1)"
if ! python3 - "$initial_telemetry" "$moving_telemetry" <<'PY'
import math, re, sys
pattern = re.compile(r"PLAYER_STATE POS=(-?[0-9.]+),(-?[0-9.]+) CAM=(-?[0-9.]+),(-?[0-9.]+) PARTS=(\d+) GL=0x([0-9a-fA-F]+)")
def parse(value):
    match = pattern.search(value)
    if not match: raise SystemExit("Malformed renderer telemetry: " + value)
    x,z,cx,cz,parts,gl_error = match.groups()
    if int(parts) != 60 or int(gl_error,16) != 0: raise SystemExit("Player batch did not complete cleanly: " + value)
    return tuple(map(float,(x,z,cx,cz)))
a,b = parse(sys.argv[1]),parse(sys.argv[2])
dx,dz = b[0]-a[0],b[1]-a[1]
camera_dx,camera_dz = b[2]-a[2],b[3]-a[3]
distance = math.hypot(dx,dz)
if distance < 0.75: raise SystemExit(f"Joystick moved the player only {distance:.2f} m")
if math.hypot(camera_dx-dx,camera_dz-dz) > 0.15: raise SystemExit("Third-person camera did not follow player translation")
print(f"Verified 60 player parts, GL_NO_ERROR, {distance:.2f} m movement and matching camera translation.")
PY
then
  echo "Player movement or camera follow verification failed."
  exit 1
fi

adb exec-out screencap -p > emulator-output/valle-cernego-moving.png
test -s emulator-output/valle-cernego-moving.png
adb logcat -d -v time > emulator-output/logcat.txt
if grep -E "FATAL EXCEPTION.*Process: es.valledecernego.game|Fatal signal.*es.valledecernego.game|ValleCernego.*OpenGL 0x" emulator-output/logcat.txt; then
  echo "Fatal app or player OpenGL error detected."
  exit 1
fi
echo "Emulator smoke test completed: 60 player parts, GL_NO_ERROR, joystick movement and camera follow verified."
