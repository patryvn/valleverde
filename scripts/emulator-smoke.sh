#!/usr/bin/env bash
set -Eeuo pipefail

mkdir -p emulator-output

collect_diagnostics() {
  local exit_code=$?
  if command -v adb >/dev/null 2>&1; then
    adb shell getprop > emulator-output/getprop.txt 2>&1 || true
    adb logcat -b all -d -v time > emulator-output/logcat.txt 2>&1 || true
    adb shell dumpsys activity activities > emulator-output/activities.txt 2>&1 || true
    adb shell dumpsys window windows > emulator-output/windows.txt 2>&1 || true
  fi
  exit "$exit_code"
}
trap collect_diagnostics EXIT

adb wait-for-device

boot_ready=0
for attempt in $(seq 1 180); do
  boot_completed="$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
  boot_animation="$(adb shell getprop init.svc.bootanim 2>/dev/null | tr -d '\r' || true)"
  if [[ "$boot_completed" == "1" && "$boot_animation" == "stopped" ]]; then
    boot_ready=1
    break
  fi
  sleep 2
done
if [[ "$boot_ready" != "1" ]]; then
  echo "Android did not finish booting within six minutes."
  exit 1
fi

package_ready=0
for attempt in $(seq 1 90); do
  if adb shell cmd package list packages 2>/dev/null | grep -q '^package:'; then
    package_ready=1
    break
  fi
  sleep 2
done
if [[ "$package_ready" != "1" ]]; then
  echo "PackageManager did not become ready after Android boot."
  exit 1
fi

adb logcat -c
install_ok=0
for attempt in $(seq 1 5); do
  if adb install -r app/build/outputs/apk/debug/app-debug.apk; then
    install_ok=1
    break
  fi
  sleep 10
done
if [[ "$install_ok" != "1" ]]; then
  echo "APK installation failed after five attempts."
  exit 1
fi

adb shell input keyevent 224 || true
adb shell input keyevent 82 || true
adb shell am force-stop es.valledecernego.game || true
adb shell am start -W -n es.valledecernego.game/.MainActivity --ez debug_telemetry true

app_ready=0
for attempt in $(seq 1 90); do
  app_pid="$(adb shell pidof es.valledecernego.game 2>/dev/null | tr -d '\r' || true)"
  resumed="$(adb shell dumpsys activity activities 2>/dev/null | grep -E 'mResumedActivity|topResumedActivity' || true)"
  focused="$(adb shell dumpsys window 2>/dev/null | grep -E 'mCurrentFocus|mFocusedApp' || true)"
  if [[ -n "$app_pid" ]] && printf '%s\n%s\n' "$resumed" "$focused" | grep -Fq 'es.valledecernego.game'; then
    app_ready=1
    break
  fi
  sleep 2
done
if [[ "$app_ready" != "1" ]]; then
  echo "MainActivity did not remain in the foreground."
  exit 1
fi

# Do not take the baseline until the GL renderer has really submitted every part.
hud_ready=0
for attempt in $(seq 1 60); do
  adb shell uiautomator dump /data/local/tmp/window.xml >/dev/null 2>&1 || true
  adb pull /data/local/tmp/window.xml emulator-output/window.xml >/dev/null 2>&1 || true
  if grep -Fq "VALLE DE CERNEGO" emulator-output/window.xml \
      && grep -Fq "60 piezas enviadas" emulator-output/window.xml \
      && grep -Fq "GL OK" emulator-output/window.xml; then
    hud_ready=1
    break
  fi
  sleep 1
done
if [[ "$hud_ready" != "1" ]]; then
  echo "The player renderer did not reach 60 submitted pieces with GL OK."
  exit 1
fi

initial_telemetry="$(grep -oE 'POS=-?[0-9]+([.][0-9]+)?,-?[0-9]+([.][0-9]+)? CAM=-?[0-9]+([.][0-9]+)?,-?[0-9]+([.][0-9]+)?' emulator-output/window.xml | tail -n 1)"
if [[ -z "$initial_telemetry" ]]; then
  echo "Initial player/camera telemetry is missing."
  exit 1
fi
adb exec-out screencap -p > emulator-output/valle-cernego-initial.png
test -s emulator-output/valle-cernego-initial.png

screen_size="$(adb shell wm size | sed -n 's/.*: //p' | tail -n 1 | tr -d '\r')"
if [[ ! "$screen_size" =~ ^[0-9]+x[0-9]+$ ]]; then
  echo "Could not determine emulator screen size: $screen_size"
  exit 1
fi
screen_w="${screen_size%x*}"
screen_h="${screen_size#*x}"
unit=$((screen_h < screen_w * 55 / 100 ? screen_h : screen_w * 55 / 100))
joy_x=$((unit * 20 / 100))
joy_y=$((screen_h - unit * 22 / 100))
move_x=$((joy_x + unit * 10 / 100))

# Match Hud's joystick geometry exactly; keep the gesture inside its active radius.
adb shell input swipe "$joy_x" "$joy_y" "$move_x" "$joy_y" 900 0

movement_verified=0
moving_telemetry=""
for attempt in $(seq 1 20); do
  sleep 1
  adb shell uiautomator dump /data/local/tmp/window-after-move.xml >/dev/null 2>&1 || true
  adb pull /data/local/tmp/window-after-move.xml emulator-output/window-after-move.xml >/dev/null 2>&1 || true
  moving_telemetry="$(grep -oE 'POS=-?[0-9]+([.][0-9]+)?,-?[0-9]+([.][0-9]+)? CAM=-?[0-9]+([.][0-9]+)?,-?[0-9]+([.][0-9]+)?' emulator-output/window-after-move.xml | tail -n 1 || true)"
  if [[ -n "$moving_telemetry" ]]; then
    if python3 - "$initial_telemetry" "$moving_telemetry" <<'PY'
import math, re, sys
pattern = re.compile(r"POS=(-?[0-9.]+),(-?[0-9.]+) CAM=(-?[0-9.]+),(-?[0-9.]+)")
first = pattern.fullmatch(sys.argv[1])
last = pattern.fullmatch(sys.argv[2])
if not first or not last:
    raise SystemExit(2)
a = [float(value) for value in first.groups()]
b = [float(value) for value in last.groups()]
dx, dz = b[0] - a[0], b[1] - a[1]
camera_dx, camera_dz = b[2] - a[2], b[3] - a[3]
if math.hypot(dx, dz) < 0.75:
    raise SystemExit(1)
if math.hypot(camera_dx - dx, camera_dz - dz) > 0.15:
    raise SystemExit(1)
print(f"Player moved {math.hypot(dx, dz):.2f} m; camera followed by ({camera_dx:.2f}, {camera_dz:.2f}) m.")
PY
    then
      movement_verified=1
      break
    fi
  fi
done
if [[ "$movement_verified" != "1" ]]; then
  echo "Joystick movement or third-person camera follow was not verified."
  echo "Initial telemetry: $initial_telemetry"
  echo "Latest telemetry: $moving_telemetry"
  exit 1
fi

grep -F "60 piezas enviadas" emulator-output/window-after-move.xml
grep -F "GL OK" emulator-output/window-after-move.xml
adb exec-out screencap -p > emulator-output/valle-cernego-moving.png
test -s emulator-output/valle-cernego-moving.png
adb shell dumpsys gfxinfo es.valledecernego.game > emulator-output/gfxinfo.txt
adb logcat -d -v time > emulator-output/logcat.txt
if grep -E "FATAL EXCEPTION.*Process: es.valledecernego.game|DeadSystemException|ValleCernego.*OpenGL 0x" emulator-output/logcat.txt; then
  echo "Fatal app, system-server, or player OpenGL error detected."
  exit 1
fi

echo "Emulator smoke test completed: 60 model parts, player movement, and camera translation verified."
