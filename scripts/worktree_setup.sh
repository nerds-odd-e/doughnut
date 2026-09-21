#!/usr/bin/env bash
#
# Small public preparation entry point for a fresh worktree.
#
# A freshly created worktree has tracked source (.agents/skills/*) but is
# missing the generated, gitignored Claude discovery links (.claude/skills/*)
# that scripts/nix_shell_hook.sh normally creates as part of the full
# interactive Nix shell hook, and its committed dependency graph
# (pnpm-lock.yaml/package.json) is not yet installed into node_modules. This
# entry point composes the existing setup_claude_skills function
# (scripts/shell_setup.sh) with the existing setup_pnpm_deps function
# (scripts/dev_setup.sh) on their own, so both can be run outside the full
# interactive hook (which also patches/restarts Biome and starts
# MySQL/Redis). Runtime commands (backend tests, frontend tests, the dev
# server) keep their own lazy service/database provisioning; this entry
# point does not start any service.
#
# Safe to run repeatedly: setup_claude_skills only adds missing symlinks and
# drops stale ones; setup_pnpm_deps only reinstalls when its workspace
# fingerprint changes.
#
# Usage: ./scripts/run.sh bash scripts/worktree_setup.sh

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"

source "${repo_root}/scripts/shell_setup.sh"
source "${repo_root}/scripts/dev_setup.sh"

# setup_pnpm_deps calls the shared log() helper; define it before use (same
# order as scripts/nix_shell_hook.sh).
setup_logging

cd "${repo_root}"
setup_claude_skills
setup_pnpm_deps
