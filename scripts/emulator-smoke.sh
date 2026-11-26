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

# Android 15 can create the immersive confirmation window before it has
# drawn its surface. Tapping while mDrawState=NO_SURFACE is ignored, so wait
# for the actual "Got it" overlay to be visible before sending the tap.
immersive_prompt_visible() {
  printf '%s\n' "$1" | awk '
    /ImmersiveModeConfirmation/ { in_prompt=1; next }
    in_prompt && /^  Window #[0-9]+ Window/ { in_prompt=0 }
    in_prompt && /Surface: shown=true/ { shown=1 }
    END { exit !shown }
  '
}
prompt_visible=0
for attempt in $(seq 1 30); do
  window_dump="$(adb shell dumpsys window windows 2>/dev/null || true)"
  if immersive_prompt_visible "$window_dump"; then prompt_visible=1; break; fi
  sleep 1
done
if [[ "$prompt_visible" == "1" ]]; then
  for attempt in $(seq 1 8); do
    adb shell input tap "$((screen_w * 70 / 100))" "$((screen_h * 35 / 100))"
    sleep 1
    window_dump="$(adb shell dumpsys window windows 2>/dev/null || true)"
    if ! immersive_prompt_visible "$window_dump"; then break; fi
  done
fi
window_dump="$(adb shell dumpsys window windows 2>/dev/null || true)"
if immersive_prompt_visible "$window_dump"; then
  echo "Android immersive-mode confirmation still covers the game after tapping its visible surface."
  printf '%s\n' "$window_dump" | grep -C 2 'ImmersiveModeConfirmation' || true
  exit 1
fi
activity_dump="$(adb shell dumpsys activity activities 2>/dev/null || true)"
if printf '%s\n' "$activity_dump" | grep -q 'mCurrentFocus=Window{.*ImmersiveModeConfirmation'; then
  echo "Immersive confirmation still owns input focus after its surface was dismissed."
  printf '%s\n' "$activity_dump" | grep 'mCurrentFocus' || true
  exit 1
fi

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
  touch_down_line="$(grep -E 'HUD_TOUCH DOWN .*mode=1' emulator-output/renderer-live.log | tail -n 1 || true)"
  touch_move_line="$(grep -E 'HUD_TOUCH MOVE .*mode=1' emulator-output/renderer-live.log | tail -n 1 || true)"
  if [[ -z "$touch_down_line" || -z "$touch_move_line" ]]; then
    echo "Touchscreen swipe did not reach the joystick handler (down=\${touch_down_line:+yes}, move=\${touch_move_line:+yes})."
    continue
  fi
  moving_line="$(grep -E 'PLAYER_STATE .*PARTS=(60|11) FALLBACK=(0|11) GL=0x0*0' emulator-output/renderer-live.log | tail -n 1 || true)"
  if [[ -z "$moving_line" ]]; then continue; fi
  moving_telemetry="$(printf '%s\n' "$moving_line" | sed -n 's/.*PLAYER_STATE /PLAYER_STATE /p' | tail -n 1)"
  if python3 - "$initial_telemetry" "$moving_telemetry" <<'PY'
import math, re, sys
pattern = re.compile(r"PLAYER_STATE POS=(-?[0-9.]+),(-?[0-9.]+) CAM=(-?[0-9.]+),(-?[0-9.]+) PARTS=(\d+) FALLBACK=(\d+) GL=0x([0-9a-fA-F]+)")
def parse(value):
    match = pattern.search(value)
    if not match: raise SystemExit("Malformed renderer telemetry: " + value)
    x,z,cx,cz,parts,fallback,gl_error = match.groups()
    if int(parts) not in (11,60) or int(fallback) not in (0,11) or (int(parts)==11 and int(fallback)!=11) or int(gl_error,16) != 0:
        raise SystemExit("Player or compatibility silhouette batch did not complete cleanly: " + value)
    return tuple(map(float,(x,z,cx,cz))) + (int(parts),int(fallback))
a,b = parse(sys.argv[1]),parse(sys.argv[2])
dx,dz = b[0]-a[0],b[1]-a[1]
camera_dx,camera_dz = b[2]-a[2],b[3]-a[3]
distance = math.hypot(dx,dz)
if distance < 0.75: raise SystemExit(f"Joystick moved the player only {distance:.2f} m")
if math.hypot(camera_dx-dx,camera_dz-dz) > 0.15: raise SystemExit("Third-person camera did not follow player translation")
print(f"Verified {b[4]} main player parts, {b[5]} compatibility parts, GL_NO_ERROR, {distance:.2f} m movement and matching camera translation.")
PY
  then
    movement_ok=1
    break
  fi
done
if [[ "$movement_ok" != "1" ]]; then
  echo "Player movement or camera follow verification failed after eight measured touchscreen gestures."
  grep -E 'HUD_LAYOUT|HUD_TOUCH|PLAYER_STATE' emulator-output/renderer-live.log | tail -n 100 || true
  exit 1
fi
adb exec-out screencap -p > emulator-output/valle-cernego-moving.png
test -s emulator-output/valle-cernego-moving.png
python3 scripts/verify-player-screenshot.py emulator-output/valle-cernego-moving.png

adb logcat -d -v time > emulator-output/logcat.txt
if grep -E "FATAL EXCEPTION.*Process: es.valledecernego.game|Fatal signal.*es.valledecernego.game|ValleCernego.*OpenGL 0x" emulator-output/logcat.txt; then
  echo "Fatal app or player OpenGL error detected."
  exit 1
fi
echo "Emulator smoke test completed: 60 player parts, GL_NO_ERROR, joystick movement and camera follow verified."
