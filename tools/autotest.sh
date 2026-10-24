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
timeout ${AUTOTEST_TIMEOUT:-1500} xvfb-run -a -s "-screen 0 1280x720x24" ./gradlew runClient --console=plain \
  --args="--width 1280 --height 720" > "$LOG" 2>&1
grep -E "AUTOTEST|NOCLIP\).*(ERROR|WARN|failed)|Exception in|Caused by|\[Render thread/ERROR\]|\[Server thread/ERROR\]" "$LOG" \
  | grep -v -E "yggdrasil|sessionserver|minecraftservices|SoundSystem|OpenAL|UnknownHost|authlib" | head -40
ls run/screenshots/ 2>/dev/null | grep autotest | tr '\n' ' '; echo
