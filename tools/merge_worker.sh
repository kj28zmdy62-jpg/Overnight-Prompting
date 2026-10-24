#!/bin/bash
# Merge a worker branch. Conflicts in generated files are resolved by regenerating;
# conflicts in real sources stop the merge for a human to look at.
#   tools/merge_worker.sh <branch>
set -u
cd "$(dirname "$0")/.."
BR="$1"
git merge --no-ff --no-commit "$BR" >/dev/null 2>&1
CONFLICTS=$(git diff --name-only --diff-filter=U)
REAL=""
for f in $CONFLICTS; do
  case "$f" in
    src/main/resources/assets/*|src/main/resources/data/*|src/main/resources/noclip/*|\
    src/main/java/com/noclip/levels/LevelIndex.java|src/main/java/com/noclip/entity/EntityIndex.java|\
    src/client/java/com/noclip/client/render/entities/ClientEntityIndex.java|\
    src/client/java/com/noclip/client/model/GeneratedModels.java)
      git checkout --theirs -- "$f" 2>/dev/null || git rm -q --cached "$f"
      git add "$f" 2>/dev/null ;;
    *) REAL="$REAL $f" ;;
  esac
done
if [ -n "$REAL" ]; then
  echo "MANUAL CONFLICTS:$REAL"
  exit 2
fi
python3 tools/build.py | tail -2 || exit 3
./gradlew compileJava compileClientJava -q --console=plain 2>&1 | grep -E "error|\.java:" && exit 4
git add -A
git commit -q -m "Merge levels from $BR

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01Se4nBZf3VSYc5tfdT4kJ89"
echo "merged $BR"
