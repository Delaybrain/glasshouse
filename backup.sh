#!/bin/sh
# Saves the home screen's choices (apps and their order, looks, status bar, screensaver, kids lock) from
# the TV to tvhome-backup.json here. restore.sh puts them back, on this TV or another.
# Usage: sh backup.sh <TV_IP> [file]
set -e
[ -n "$1" ] || { echo "Usage: sh backup.sh <TV_IP> [file]" >&2; exit 1; }
case "$1" in
  *.*.*.*:*) TV=$1 ;;
  *.*.*.*)   TV=$1:5555 ;;
  *)         TV=$1 ;;
esac
OUT=${2:-tvhome-backup.json}
case "$TV" in *:*) adb connect "$TV" >/dev/null || true ;; esac
[ "$(adb -s "$TV" get-state 2>/dev/null)" = device ] || { echo "Can't reach $TV." >&2; exit 1; }

# The home screen writes the backup itself (Settings > Back up & restore does the same).
FILE=/sdcard/Android/data/com.ansub.tvhome/files/tvhome-backup.json
adb -s "$TV" shell "rm -f $FILE"
adb -s "$TV" shell am start -n com.ansub.tvhome/.MainActivity --es backup save >/dev/null 2>&1
for i in 1 2 3 4 5; do
  adb -s "$TV" shell "[ -s $FILE ]" && break
  sleep 1
done
# Git Bash on Windows turns an argument that starts with / into a Windows path: keep it in one string.
MSYS_NO_PATHCONV=1 adb -s "$TV" pull "$FILE" "$OUT" >/dev/null || { echo "The TV didn't write a backup." >&2; exit 1; }
echo "Saved to $OUT"
