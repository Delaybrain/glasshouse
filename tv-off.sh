#!/bin/sh
# Turns apps off on the TV, one by one, and writes each one to an undo list first.
#   sh tv-off.sh <TV_IP:port> <undo-list.txt> pkg1 pkg2 ...
# The list is in check.sh's format ("off <package>"), so check.sh --fix can keep them off after
# an update, and every line can be undone with:  adb shell pm enable <package>
set -e
TV="$1"; LIST="$2"; shift 2
for p in "$@"; do
  if ! adb -s "$TV" shell pm list packages "$p" | tr -d '\r' | grep -qx "package:$p"; then
    echo "skip  $p (not installed)"; continue
  fi
  flags=$(adb -s "$TV" shell dumpsys package "$p" | tr -d '\r' | sed -n '/pkgFlags=\[/{p;q;}')
  case "$flags" in *PERSISTENT*) echo "skip  $p (PERSISTENT, would not stay off)"; continue ;; esac
  grep -qx "off $p" "$LIST" 2>/dev/null || echo "off $p" >> "$LIST"
  out=$(adb -s "$TV" shell pm disable-user --user 0 "$p" | tr -d '\r')
  case "$out" in *disabled-user*) echo "off   $p" ;; *) echo "FAIL  $p: $out" ;; esac
done
