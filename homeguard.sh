#!/bin/sh
# Turns HomeGuard on or off: on Google TV it brings this home screen up whenever Google TV's own
# Home page comes to the front, so Google TV's home screen can stay on (turning it off is what left
# some Google TVs stuck restarting). Run install.sh first.
#
#   sh homeguard.sh <TV_IP> on
#   sh homeguard.sh <TV_IP> off
set -e
TARGET="$1"; MODE="$2"
case "$MODE" in on|off) ;; *) echo "Usage: sh homeguard.sh <TV_IP> on|off" >&2; exit 1 ;; esac
case "$TARGET" in
  *.*.*.*:*) TV=$TARGET ;;
  *.*.*.*)   TV=$TARGET:5555 ;;
  *)         TV=$TARGET ;;
esac
case "$TV" in *:*) adb connect "$TV" >/dev/null || true ;; esac
[ "$(adb -s "$TV" get-state 2>/dev/null)" = device ] || { echo "Can't reach $TV." >&2; exit 1; }

ME=com.ansub.tvhome/.HomeGuard
sh_tv() { adb -s "$TV" shell "$@" </dev/null | tr -d '\r'; }
sh_tv pm list packages com.ansub.tvhome | grep -qx package:com.ansub.tvhome \
  || { echo "The home screen isn't installed. Run install.sh first." >&2; exit 1; }

# Other accessibility services (like TalkBack) stay as they are: only ours is added or taken out.
now=$(sh_tv settings get secure enabled_accessibility_services)
[ "$now" = null ] && now=
rest=$(printf '%s' "$now" | tr ':' '\n' | grep -vx "$ME" | grep -v '^$' | paste -sd: - || true)
if [ "$MODE" = on ]; then
  list=${rest:+$rest:}$ME
  sh_tv settings put secure enabled_accessibility_services "$list"
  sh_tv settings put secure accessibility_enabled 1
  sh_tv settings delete secure tvhome_homeguard >/dev/null # the home screen may put it back again
  echo "HomeGuard is on. Press Home on the remote to try it."
else
  if [ -n "$rest" ]; then sh_tv settings put secure enabled_accessibility_services "$rest"
  else sh_tv settings delete secure enabled_accessibility_services >/dev/null
       sh_tv settings put secure accessibility_enabled 0; fi
  # Tells the home screen not to turn it back on by itself (it does that after updates).
  sh_tv settings put secure tvhome_homeguard off
  echo "HomeGuard is off. Home opens the TV's own home screen again."
fi
