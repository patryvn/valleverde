#!/usr/bin/env bash
set -Eeuo pipefail

mkdir -p emulator-output

# Always preserve system and GL diagnostics, including when a command fails.
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
adb shell am start -W -n es.valledecernego.game/.MainActivity

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

sleep 10
adb exec-out screencap -p > emulator-output/valle-cernego-initial.png
test -s emulator-output/valle-cernego-initial.png
adb shell uiautomator dump /data/local/tmp/window.xml
adb pull /data/local/tmp/window.xml emulator-output/window.xml
grep -F "VALLE DE CERNEGO" emulator-output/window.xml
grep -F "60 piezas enviadas" emulator-output/window.xml

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

# Android 15/35 input requires an explicit display ID after the duration.
adb shell input swipe "$joy_x" "$joy_y" "$move_x" "$joy_y" 900 0
sleep 3
adb exec-out screencap -p > emulator-output/valle-cernego-moving.png
test -s emulator-output/valle-cernego-moving.png
if cmp -s emulator-output/valle-cernego-initial.png emulator-output/valle-cernego-moving.png; then
  echo "The scene capture did not change after the joystick gesture."
  exit 1
fi

adb shell dumpsys gfxinfo es.valledecernego.game > emulator-output/gfxinfo.txt
if grep -E "FATAL EXCEPTION.*Process: es.valledecernego.game|DeadSystemException|ValleCernego.*OpenGL 0x" emulator-output/logcat.txt; then
  echo "Fatal app, system-server, or player OpenGL error detected."
  exit 1
fi

echo "Emulator smoke test completed; inspect both PNG captures before accepting player visibility."
