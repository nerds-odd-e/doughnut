#!/usr/bin/env bash
#
# Times the installed pre-commit hook on a fixed corpus of staged changes.
#
# Usage (outside Nix, with the host Bash):
#   bash scripts/profiling/profile-commit-hook.sh baseline <evidence-directory>
#   bash scripts/profiling/profile-commit-hook.sh acceptance <evidence-directory>
#
# The driver owns one disposable linked worktree, detached at this checkout's
# committed HEAD under a physical temporary path, prepared through the public
# scripts/worktree_setup.sh. Each case resets that worktree to HEAD, stages its
# change and runs the fixture's tracked hook through Git's hook dispatch
# (`git -c core.hooksPath=<fixture>/scripts/git-hooks hook run pre-commit`),
# so the shared .git/hooks installation is never replaced. Timing covers hook
# invocation to exit, including Nix entry.
#
# Corpus (commit-hook-corpus.sh): 8 classes x 5 distinct staged changes.
# Diagnostics outside the 40: a fresh-cache run per class (CHECK_CACHE_PATHS
# removed first), one per class already inside Nix, and a documentation-only
# no-component run.
# Every run's output, exit status, input identities and source/index
# preservation check stay in the evidence directory; nothing is dropped.
#
# Compatible with macOS /bin/bash 3.2.

set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/commit-hook-corpus.sh"

# Reusable check state inside the fixture that a fresh-cache run removes.
# Shared state outside the fixture (Gradle user home and daemon, pnpm store,
# Nix store and evaluation cache) and installed dependencies are kept.
CHECK_CACHE_PATHS="dist backend/build backend/.gradle"

UNTRACKED_PROBE="profile-commit-hook-untracked.txt"
UNSTAGED_PROBE="docs/tech_stack.md"
NO_COMPONENT_PATH="docs/nix.md"

now() {
  perl -MTime::HiRes=time -e 'printf "%.3f\n", time'
}

# Fixture back to HEAD plus crafted unstaged and untracked work that every
# hook run must leave alone.
reset_fixture() {
  git -C "$fixture" reset -q --hard HEAD
  git -C "$fixture" clean -fdq
  printf 'unstaged profile edit\n' >>"$fixture/$UNSTAGED_PROBE"
  printf 'untracked profile content\n' >"$fixture/$UNTRACKED_PROBE"
}

stage_case() {
  local number="$1" file
  shift
  for file in "$@"; do
    change_file "$fixture/$file" "$number"
    git -C "$fixture" add -- "$file"
  done
  if git -C "$fixture" diff --cached --quiet -- "$@"; then
    echo "Case $number staged no change in: $*" >&2
    exit 1
  fi
}

input_identities() {
  git -C "$fixture" ls-files --stage -- "$@" | awk '{ printf "%s%s:%s", sep, $4, $2; sep = "," }'
}

snapshot() {
  (
    cd "$fixture"
    echo '## status'
    git status --porcelain=v1 --untracked-files=all
    echo '## index'
    git ls-files --stage
    echo '## staged'
    git diff --cached --binary
    echo '## working tree'
    git diff --binary
    echo '## untracked contents'
    git ls-files --others --exclude-standard | while IFS= read -r file; do
      printf '%s %s\n' "$(git hash-object -- "$file")" "$file"
    done
  )
}

# Runs the hook as Git dispatches it and records its outcome in a case
# directory. Invoked directly, or as `run-hook` from inside Nix.
run_hook() {
  local fixture_dir="$1" case_dir="$2" start finish status
  start="$(now)"
  set +e
  (cd "$fixture_dir" && git -c core.hooksPath="$fixture_dir/scripts/git-hooks" hook run pre-commit) \
    >"$case_dir/hook.stdout" 2>"$case_dir/hook.stderr" </dev/null
  status=$?
  set -e
  finish="$(now)"
  echo "$status" >"$case_dir/exit-status"
  awk -v s="$start" -v f="$finish" 'BEGIN { printf "%.3f\n", f - s }' >"$case_dir/seconds"
}

# record <section> <case-id> <class> <entry> <path>...
record() {
  local section="$1" case_id="$2" class="$3" entry="$4" case_dir
  shift 4
  case_dir="$evidence/$section/$case_id"
  mkdir -p "$case_dir"
  snapshot >"$case_dir/before.snapshot"
  if [[ "$entry" == inside-nix ]]; then
    (cd "$fixture" && CURSOR_DEV=true nix develop -c bash "$driver" run-hook "$fixture" "$case_dir") \
      >"$case_dir/nix-entry.log" 2>&1 </dev/null
  else
    run_hook "$fixture" "$case_dir"
  fi
  snapshot >"$case_dir/after.snapshot"

  local preserved=yes warnings
  if ! diff "$case_dir/before.snapshot" "$case_dir/after.snapshot" >"$case_dir/preservation.diff"; then
    preserved=no
  fi
  warnings="$(cat "$case_dir/hook.stdout" "$case_dir/hook.stderr" | grep -ciE 'warn|ERR_PNPM' || true)"
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "$section" "$case_id" "$class" "$(cat "$case_dir/seconds")" \
    "$(cat "$case_dir/exit-status")" "$warnings" "$preserved" \
    "$(input_identities "$@")" >>"$evidence/results.tsv"
}

run_case() {
  local section="$1" entry="$2" number="$3" case_id="$4" class="$5"
  shift 5
  reset_fixture
  stage_case "$number" "$@"
  record "$section" "$case_id" "$class" "$entry" "$@"
}

# run_section <section> <entry> every|first: the whole corpus, or each
# class's first case. Case numbers follow corpus order in every section.
run_section() {
  local section="$1" entry="$2" which="$3" number=0 case_id class paths
  while read -r case_id class paths; do
    number=$((number + 1))
    [[ "$which" == every || "$case_id" == "$class-1" ]] || continue
    if [[ "$section" == fresh-cache ]]; then
      (cd "$fixture" && rm -rf $CHECK_CACHE_PATHS)
    fi
    run_case "$section" "$entry" "$number" "$case_id" "$class" $paths
  done < <(corpus)
}

record_environment() {
  {
    echo "mode: $mode"
    echo "date: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
    echo "revision: $(git -C "$repo_root" rev-parse HEAD)"
    echo "fixture: $fixture"
    echo "host bash: $BASH_VERSION"
    echo "uname: $(uname -a)"
    if command -v sw_vers >/dev/null; then sw_vers; fi
    if command -v sysctl >/dev/null; then sysctl -n machdep.cpu.brand_string hw.memsize 2>/dev/null || true; fi
    echo "git: $(git --version)"
    echo "nix: $(nix --version)"
    echo "load before: $(uptime)"
    echo "reset check caches: $CHECK_CACHE_PATHS"
    echo "tools in the fixture environment:"
    (cd "$fixture" && ./scripts/run.sh bash -c 'node --version; pnpm --version; java -version 2>&1 | head -1')
  } >"$evidence/environment.txt" 2>&1
}

summarize() {
  awk -F'\t' -v mode="$mode" '
    $1 == "corpus" {
      n++; total += $4; times = times sprintf(" %s=%s", $2, $4)
      count[$3]++; sum[$3] += $4
      if (!($3 in min) || $4 < min[$3]) min[$3] = $4
      if (!($3 in max) || $4 > max[$3]) max[$3] = $4
      if (!(seen[$3]++)) order[++classes] = $3
    }
    NR > 1 && ($5 != 0 || $6 != 0 || $7 != "yes") {
      problems = problems sprintf("\n  %s %s exit=%s warnings=%s preserved=%s", $1, $2, $5, $6, $7)
    }
    END {
      printf "mode: %s\ncorpus runs: %d\ntimings:%s\n", mode, n, times
      printf "overall mean: %.3f s\n", total / n
      for (i = 1; i <= classes; i++) {
        c = order[i]
        printf "class %s: mean %.3f s, range %.3f-%.3f s\n", c, sum[c] / count[c], min[c], max[c]
      }
      printf "problem runs (failure, warning or changed source/index):%s\n", (problems == "" ? " none" : problems)
      printf "per-run logs: %s/<section>/<case>/\n", evidence
      exit (problems != "")
    }' evidence="$evidence" "$evidence/results.tsv"
}

cleanup() {
  if [[ -d "$fixture" ]]; then
    git -C "$repo_root" worktree remove --force "$fixture"
  fi
  rm -rf "$fixture_parent"
}

main() {
  mode="${1:-}"
  evidence="${2:-}"
  case "$mode" in
    baseline|acceptance) ;;
    run-hook)
      run_hook "$2" "$3"
      return
      ;;
    *)
      echo "Usage: $0 baseline|acceptance <evidence-directory>" >&2
      exit 1
      ;;
  esac
  [[ -n "$evidence" ]] || { echo "Missing evidence directory" >&2; exit 1; }
  if [[ -n "${IN_NIX_SHELL:-}" ]]; then
    echo "Run outside Nix: the measured hook must include its own Nix entry." >&2
    exit 1
  fi

  driver="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)/$(basename "${BASH_SOURCE[0]}")"
  repo_root="$(git -C "$(dirname "$driver")" rev-parse --show-toplevel)"
  mkdir -p "$evidence"
  evidence="$(cd "$evidence" && pwd -P)"
  rm -f "$evidence/results.tsv"
  printf 'section\tcase\tclass\tseconds\texit\twarnings\tpreserved\tinputs\n' >"$evidence/results.tsv"

  fixture_parent="$(mktemp -d "${TMPDIR:-/tmp}/donut-profile-commit-hook.XXXXXX")"
  fixture_parent="$(cd "$fixture_parent" && pwd -P)"
  fixture="$fixture_parent/checkout"
  trap cleanup EXIT
  git -C "$repo_root" worktree add -q --detach "$fixture" HEAD
  (cd "$fixture" && ./scripts/run.sh bash scripts/worktree_setup.sh) >"$evidence/preparation.log" 2>&1
  record_environment

  # Fresh-cache diagnostics first; they also warm the fixture for the corpus.
  run_section fresh-cache outside-nix first
  run_section corpus outside-nix every
  run_section inside-nix inside-nix first
  run_case no-component outside-nix 0 documentation none "$NO_COMPONENT_PATH"

  echo "load after: $(uptime)" >>"$evidence/environment.txt"
  summarize | tee "$evidence/summary.txt"
}

main "$@"
