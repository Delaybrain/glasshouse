#!/bin/sh
# Puts the TV back the way it came: every app on the change list on again, every changed setting back
# to its old value, then undo.sh (Google TV's home screen on, this home screen removed).
#
#   sh restore-stock.sh <TV_IP> <change-list.txt>
#   DRY=1 sh restore-stock.sh <TV_IP> <change-list.txt>    only prints what it would do
#
# The list is the one check.sh reads: "off <package>" lines are turned back on, and each
# "# vorher: <global|secure|system|cec> <name> <value>" line is the value a setting had before
# ("null" means it wasn't set at all). Nothing is uninstalled except this home screen.
set -e
TARGET="$1"; LIST="$2"; HERE="$(dirname "$0")"
[ -n "$TARGET" ] && [ -f "$LIST" ] || { echo "Usage: sh restore-stock.sh <TV_IP> <change-list.txt>" >&2; exit 1; }
case "$TARGET" in
  *.*.*.*:*) TV=$TARGET ;;
  *.*.*.*)   TV=$TARGET:5555 ;;
  *)         TV=$TARGET ;;
esac

run() { # prints each change; with DRY=1 that's all it does
  echo "  $*"
  [ -n "$DRY" ] || adb -s "$TV" shell "$@" </dev/null >/dev/null 2>&1 || echo "    (failed, carrying on)"
}

if [ -z "$DRY" ]; then
  case "$TV" in *:*) adb connect "$TV" >/dev/null || true ;; esac
  [ "$(adb -s "$TV" get-state 2>/dev/null)" = device ] || { echo "Can't reach $TV." >&2; exit 1; }
  printf "This turns every app on the list back on, puts the old settings back and removes this home screen. Go ahead? [y/N] "
  answer=${YES:+y}
  [ -n "$answer" ] || read answer </dev/tty || answer=n
  case "$answer" in y|Y|yes) ;; *) echo "Nothing changed."; exit 0 ;; esac
fi

echo "Apps back on:"
grep '^off ' "$LIST" | while read -r _ pkg; do run pm enable "$pkg"; done

echo "Settings back to what they were:"
sed -n 's/^# vorher: //p' "$LIST" | while read -r kind name value; do
  case "$kind" in
    global|secure|system)
      if [ "$value" = null ]; then run settings delete "$kind" "$name"; else run settings put "$kind" "$name" "$value"; fi ;;
    cec) run cmd hdmi_control cec_setting set "$name" "$value" ;;
  esac
done

echo "Home screen:"
if [ -n "$DRY" ]; then echo "  sh undo.sh $TV"; else sh "$HERE/undo.sh" "$TV"; fi

echo "Done. Restart the TV once (adb -s $TV reboot) so everything starts the way it used to."
