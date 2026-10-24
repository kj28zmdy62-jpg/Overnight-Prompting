#!/bin/bash
# Boots the real client headlessly, visits the given levels and screenshots them.
#   tools/autotest.sh level_0,level_1 [flash]
# Screenshots land in run/screenshots/autotest_<level>_<n>.png
cd "$(dirname "$0")/.."
rm -f run/screenshots/autotest_*.png
rm -rf run/saves/autotest
export NOCLIP_AUTOTEST="$1"
[ "$2" = "flash" ] && export NOCLIP_AUTOTEST_FLASH=1
export LIBGL_ALWAYS_SOFTWARE=1
LOG=run/autotest.log
# At most two game clients run at once across every worktree (CPU and RAM are shared).
exec 8>/tmp/noclip-autotest-a.lock 9>/tmp/noclip-autotest-b.lock
if ! flock -n 8; then
  if ! flock -n 9; then
    echo "waiting for a free autotest slot..."
    flock 8
  fi
fi
W=${AUTOTEST_W:-1280}; H=${AUTOTEST_H:-720}
timeout ${AUTOTEST_TIMEOUT:-1500} xvfb-run -a -s "-screen 0 ${W}x${H}x24" ./gradlew runClient --console=plain \
  --args="--width $W --height $H" > "$LOG" 2>&1
grep -E "AUTOTEST|NOCLIP\).*(ERROR|WARN|failed)|Exception in|Caused by|\[Render thread/ERROR\]|\[Server thread/ERROR\]" "$LOG" \
  | grep -v -E "yggdrasil|sessionserver|minecraftservices|SoundSystem|OpenAL|UnknownHost|authlib" | head -40
ls run/screenshots/ 2>/dev/null | grep autotest | tr '\n' ' '; echo
