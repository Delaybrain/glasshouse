#!/bin/sh
# Installs the home screen on an Android TV (real or emulator) exactly like a user would,
# then checks it works. Usage: sh ci/smoke-test.sh <adb serial or TV_IP> [screenshot.png]
set -e
cd "$(dirname "$0")/.."
TV="$1"; SHOT="${2:-smoke.png}"
fail() {
  echo "FAIL: $*" >&2
  echo "-- on screen: $(focus)" >&2
  adb -s "$TV" logcat -d -b crash 2>/dev/null | grep -A12 "FATAL EXCEPTION" | head -40 >&2
  adb -s "$TV" exec-out screencap -p > "${SHOT%.png}-fail.png" 2>/dev/null || true
  exit 1
}
# Waits up to 15s for the home screen to be in front.
wait_home() { for _ in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15; do focus | grep -q com.delaybrain.glasshouse && return 0; sleep 1; done; return 1; }
ui() { adb -s "$TV" shell "uiautomator dump /sdcard/ui.xml" >/dev/null 2>&1; adb -s "$TV" shell "cat /sdcard/ui.xml"; }
focus() { adb -s "$TV" shell dumpsys window | grep -m1 mCurrentFocus; }

echo "== Android $(adb -s "$TV" shell getprop ro.build.version.release | tr -d '\r'), $(adb -s "$TV" shell wm size | tail -1 | tr -d '\r')"

echo "== install"
YES=1 sh install.sh "$TV"
sleep 3
home=$(adb -s "$TV" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1 | tr -d '\r')
[ "$home" = "com.delaybrain.glasshouse/.MainActivity" ] || fail "default home is $home"
adb -s "$TV" shell input keyevent KEYCODE_HOME
wait_home || fail "home screen not in front"

echo "== home screen shows a clock and Settings"
x=$(ui)
echo "$x" | grep -q 'text="Settings"' || fail "no Settings tile"
echo "$x" | grep -qE 'text="[0-9]{1,2}:[0-9]{2}"' || fail "no clock"
tiles=$(echo "$x" | grep -o 'focusable="true"' | wc -l | tr -d ' ')
echo "   $tiles tiles"

echo "== remote: move right, open Settings, come back with Home"
adb -s "$TV" shell input keyevent KEYCODE_DPAD_RIGHT
adb -s "$TV" shell am start -a android.settings.SETTINGS >/dev/null; sleep 3
adb -s "$TV" shell input keyevent KEYCODE_HOME
wait_home || fail "Home didn't come back"

echo "== custom app list"
# Use whatever TV app this device has (test images may have none).
app=$(adb -s "$TV" shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER \
  | tr -d '\r' | grep / | sed 's|/.*||; s/^ *//' | grep -vx com.delaybrain.glasshouse | head -1)
if [ -n "$app" ]; then
  YES=1 sh install.sh "$TV" "$app"
  adb -s "$TV" shell input keyevent KEYCODE_HOME
  wait_home || fail "home screen not in front after custom list"
  n=$(ui | grep -o 'focusable="true"' | wc -l | tr -d ' ')
  [ "$n" -ge 2 ] || fail "custom app $app didn't show up"
  echo "   $app shows up"
else
  echo "   skipped: this device has no TV apps"
fi

adb -s "$TV" exec-out screencap -p > "$SHOT"

echo "== Settings > Remote buttons: give Home the red button, then back to standard"
# Taps the middle of the first element whose text starts with $1.
tap() {
  b=$(ui | tr '>' '\n' | grep "text=\"$1" | head -1 | sed -n 's/.*bounds="\[\([0-9]*\),\([0-9]*\)\]\[\([0-9]*\),\([0-9]*\)\]".*/\1 \2 \3 \4/p')
  [ -n "$b" ] || fail "nothing on screen starts with '$1'"
  set -- $b
  adb -s "$TV" shell input tap $((($1 + $3) / 2)) $((($2 + $4) / 2))
  sleep 1
}
row() { ui | tr '>' '\n' | grep -o "text=\"$1[^\"]*\"" | head -1; }
adb -s "$TV" shell am start -n com.delaybrain.glasshouse/.MainActivity --es show remote >/dev/null; sleep 2
row "Remote buttons" | grep -q . || fail "no Remote buttons panel"
row "Home  ·  " | grep -q 'Home  ·  Home"' || fail "Home doesn't start on the standard button: $(row 'Home  ·  ')"
tap "Home  ·  "
row "Press the button now" | grep -q . || fail "no wait for a button"
adb -s "$TV" shell input keyevent KEYCODE_PROG_RED; sleep 2
row "Home  ·  " | grep -q 'Home  ·  Home, Prog red"' || fail "red button not set: $(row 'Home  ·  ')"
tap "Reset to standard"
tap 'Reset"'; sleep 1  # the question before it: Reset, not Cancel
row "Home  ·  " | grep -q 'Home  ·  Home"' || fail "standard buttons not back: $(row 'Home  ·  ')"
adb -s "$TV" shell input keyevent KEYCODE_BACK
echo "   set and reset"

echo "== check.sh refuses a list it can't read"
list=$(mktemp)
echo "# nothing to check" > "$list"
sh check.sh "$TV" "$list" 2>&1 | grep -q "Can't read" && fail "check.sh refused a list it can read"
rm -f "$list"
rc=0; sh check.sh "$TV" "$list" >/dev/null 2>&1 || rc=$?
[ "$rc" = 2 ] || fail "check.sh exit $rc for a list it can't read, expected 2"

echo "== undo puts the TV back"
sh undo.sh "$TV" >/dev/null
sleep 3
home=$(adb -s "$TV" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1 | tr -d '\r')
case "$home" in *com.delaybrain.glasshouse*|"") fail "after undo, home is '$home'" ;; esac
echo "   home is back to $home"
echo "PASS ($SHOT)"
