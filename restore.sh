#!/bin/sh
# Puts a backup made with backup.sh (or on the TV) back: the home screen starts over with it.
# Usage: sh restore.sh <TV_IP> [file]
set -e
[ -n "$1" ] || { echo "Usage: sh restore.sh <TV_IP> [file]" >&2; exit 1; }
case "$1" in
  *.*.*.*:*) TV=$1 ;;
  *.*.*.*)   TV=$1:5555 ;;
  *)         TV=$1 ;;
esac
IN=${2:-glasshouse-backup.json}
[ -f "$IN" ] || { echo "No $IN here. Make one with: sh backup.sh <TV_IP>" >&2; exit 1; }
case "$TV" in *:*) adb connect "$TV" >/dev/null || true ;; esac
[ "$(adb -s "$TV" get-state 2>/dev/null)" = device ] || { echo "Can't reach $TV." >&2; exit 1; }

DIR=/sdcard/Android/data/com.delaybrain.glasshouse/files
# Start the home screen once, so its folder exists (a fresh install doesn't have it yet).
adb -s "$TV" shell am start -n com.delaybrain.glasshouse/.MainActivity >/dev/null 2>&1
adb -s "$TV" shell "mkdir -p $DIR"
MSYS_NO_PATHCONV=1 adb -s "$TV" push "$IN" "$DIR/glasshouse-backup.json" >/dev/null
adb -s "$TV" shell am start -n com.delaybrain.glasshouse/.MainActivity --es backup restore >/dev/null 2>&1
echo "Restored from $IN"
