#!/usr/bin/env bash
#
# Small public preparation entry point for a fresh worktree.
#
# A freshly created worktree has tracked source (.agents/skills/*) but is
# missing the generated, gitignored Claude discovery links (.claude/skills/*)
# that scripts/nix_shell_hook.sh normally creates as part of the full
# interactive Nix shell hook. This entry point composes the existing
# setup_claude_skills function (scripts/shell_setup.sh) on its own, so it can
# be run outside that full hook (which also starts MySQL/Redis/Biome).
#
# Safe to run repeatedly: setup_claude_skills only adds missing symlinks and
# drops stale ones.
#
# Usage: ./scripts/run.sh bash scripts/worktree_setup.sh

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"

source "${repo_root}/scripts/shell_setup.sh"

cd "${repo_root}"
setup_claude_skills
