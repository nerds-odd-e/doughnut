#!/usr/bin/env bash
# Publish-cost measurement for SEED-046#story-1 (deleted at wrap-up).
# Builds notebooks A and B through the CLI on a running Development stack, then
# times four publishes: wall time, CLI user+sys (/usr/bin/time -p) and the number
# of git processes the CLI starts (GIT_TRACE).
#
# Environment:
#   DONUT_CLI   command that runs the CLI (default: node <this checkout>/cli/dist/donut-cli.bundle.mjs)
#   BASE_URL    Development stack origin (default http://127.0.0.1:5175)
#   LOGIN       basic-auth user:password (default manual:password)
#   WORK_ROOT   scratch root (default /Users/terryyin/.claude/jobs/f1831243/tmp/measure-publish)
#   REUSE_RUN   earlier run id whose notebooks and token to measure again, skipping setup
set -euo pipefail

CHECKOUT=$(cd "$(dirname "$0")/../../.." && pwd)
DONUT_CLI=${DONUT_CLI:-"node $CHECKOUT/cli/dist/donut-cli.bundle.mjs"}
BASE_URL=${BASE_URL:-http://127.0.0.1:5175}
LOGIN=${LOGIN:-manual:password}
WORK_ROOT=${WORK_ROOT:-/Users/terryyin/.claude/jobs/f1831243/tmp/measure-publish}
RUN=$(date +%Y%m%d-%H%M%S)
SETUP_RUN=${REUSE_RUN:-$RUN}
WORK=$WORK_ROOT/$SETUP_RUN
mkdir -p "$WORK/cfg"

export DONUT_API_BASE_URL=$BASE_URL DONUT_CONFIG_DIR=$WORK/cfg
[ -n "${REUSE_RUN:-}" ] || TOKEN=$(curl -sf -u "$LOGIN" -X POST -H 'Content-Type: application/json' \
  -d "{\"label\":\"measure-$RUN\"}" "$BASE_URL/api/user/generate-token" |
  node -e 'process.stdout.write(JSON.parse(require("fs").readFileSync(0)).token)')
[ -n "${REUSE_RUN:-}" ] || printf '{"token":"%s"}\n' "$TOKEN" >"$WORK/cfg/access-tokens.json"

cli() { $DONUT_CLI "$@" >>"$WORK/cli.log" 2>&1 || { tail -5 "$WORK/cli.log"; exit 1; }; }

new_notebook() { # title -> clones into $WORK/<title>
  local id
  id=$(curl -sf -u "$LOGIN" -X POST -H 'Content-Type: application/json' \
    -d "{\"newTitle\":\"$1\"}" "$BASE_URL/api/notebooks/create" |
    node -e 'process.stdout.write(String(JSON.parse(require("fs").readFileSync(0)).notebook.id))')
  cli notebook clone "$id" "$WORK/$1"
}

files() { # dir count bytes prefix
  for i in $(seq -w 1 "$2"); do head -c "$3" /dev/urandom >"$1/$4-$i.bin"; done
}

note() { printf -- '---\ntype: Note\n---\n# %s\n\n%s\n' "$2" "$3" >"$1/$2.md"; }

commit() { git -C "$1" add -A && git -C "$1" commit -qm "$2"; }

publish() { cli notebook publish "$1"; }

note_commits() { # dir count publish-each(yes|no)
  for i in $(seq -w 1 "$2"); do
    note "$1" "n-$i" "Note $i."
    commit "$1" "note $i"
    [ "$3" = yes ] && publish "$1"
  done
  return 0
}

RESULTS=$WORK/results-$RUN.md
echo "| Run $RUN | Publish | Wall s | CLI user+sys s | CLI git processes |" >"$RESULTS"
echo "| --- | --- | --- | --- | --- |" >>"$RESULTS"

measure() { # label dir
  local trace=$WORK/trace-$RANDOM.log timing=$WORK/time.log
  GIT_TRACE=$trace /usr/bin/time -p $DONUT_CLI notebook publish "$2" >>"$WORK/cli.log" 2>"$timing" ||
    { cat "$timing"; exit 1; }
  local real user sys count
  real=$(awk '/^real /{print $2}' "$timing")
  user=$(awk '/^user /{print $2}' "$timing")
  sys=$(awk '/^sys /{print $2}' "$timing")
  count=$(grep -c 'trace: built-in: git\|trace: exec: git' "$trace" || true)
  echo "| | $1 | $real | $(echo "$user + $sys" | bc) ($user + $sys) | $count |" | tee -a "$RESULTS"
}

A=A-$SETUP_RUN
B=B-$SETUP_RUN
echo "work: $WORK"

if [ -z "${REUSE_RUN:-}" ]; then

echo "setup A: 12 x 8,912,896-byte files, then 40 note commits published one by one"
new_notebook "$A"
files "$WORK/$A" 12 8912896 a
commit "$WORK/$A" "twelve files"
publish "$WORK/$A"
note_commits "$WORK/$A" 40 yes

echo "setup B: 1,000 x 200 KB + 36 x 8.5 MB files, then 45 note commits in one publish"
new_notebook "$B"
files "$WORK/$B" 1000 204800 s
files "$WORK/$B" 36 8912896 b
commit "$WORK/$B" "many files"
publish "$WORK/$B"
note_commits "$WORK/$B" 45 no
publish "$WORK/$B"
fi

note "$WORK/$A" n-01 "Note 01, edited in $RUN."
commit "$WORK/$A" "one-line edit"
measure "A one-line edit" "$WORK/$A"

note "$WORK/$B" n-01 "Note 01, edited in $RUN."
commit "$WORK/$B" "one-line edit"
measure "B one-line edit" "$WORK/$B"

files "$WORK/$A" 12 8912896 "new-$RUN"
commit "$WORK/$A" "twelve new files"
measure "A +12 files (100 MB)" "$WORK/$A"

head -c 8912896 /dev/urandom >"$WORK/$A/a-01.bin"
commit "$WORK/$A" "one changed file"
measure "A one changed 8.5 MB file" "$WORK/$A"

echo "results: $RESULTS"
