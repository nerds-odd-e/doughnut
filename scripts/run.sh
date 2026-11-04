#!/usr/bin/env bash
#
# Smart command runner that automatically handles nix environment:
# - If nix is installed and not in a nix shell: runs the command in a
#   recorded development environment (see below) with CURSOR_DEV=true
# - If already in a nix shell or nix is not installed: runs command directly
#
# The recorded environment is a Nix profile built from a snapshot of flake.nix and
# flake.lock, kept per worktree in the Git directory and keyed by their content.
# Reusing it avoids reevaluating the source tree on every call. Local runs use
# cached dependencies. Set DONUT_ALLOW_DOWNLOADS=1 to prepare dependencies on
# an unmetered connection.
#
# Usage: ./scripts/run.sh <command> [args...]

set -e

nix_options=()
if [ "${DONUT_ALLOW_DOWNLOADS:-}" != "1" ]; then
    export DONUT_OFFLINE=1 PIP_NO_INDEX=1 npm_config_offline=true pnpm_config_offline=true CYPRESS_INSTALL_BINARY=0
    nix_options=(--offline --option substitute false --max-jobs 0 --builders "")
fi

# Check if nix is installed
if command -v nix >/dev/null 2>&1; then
    # Check if we're currently in a nix shell
    if [ -z "${IN_NIX_SHELL:-}" ]; then
        export CURSOR_DEV=true
        root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
        definition=(flake.nix flake.lock)
        key="$(cd "$root" && cat "${definition[@]}" | git hash-object --stdin)"
        base="$(git -C "$root" rev-parse --path-format=absolute --git-path donut-nix-env)"
        state="$base/$key"
        if [ ! -e "$state/profile" ] && [ "${DONUT_ALLOW_DOWNLOADS:-}" != "1" ]; then
            # Matching worktrees can reuse the primary checkout's recorded profile.
            common_dir="$(git -C "$root" rev-parse --path-format=absolute --git-common-dir)"
            state="$common_dir/donut-nix-env/$key"
            if [ ! -e "$state/profile" ]; then
                echo "No cached Nix environment. On an unmetered connection, run DONUT_ALLOW_DOWNLOADS=1 ./scripts/run.sh bash scripts/worktree_setup.sh." >&2
                exit 1
            fi
        fi
        if [ ! -e "$state/profile" ]; then
            mkdir -p "$state/flake"
            find "$base" -mindepth 1 -maxdepth 1 ! -name "$key" -exec rm -rf {} +
            # Publish each snapshot file by rename so a concurrent same-key reader never sees a partial file
            for file in "${definition[@]}"; do
                cp "$root/$file" "$state/flake/.$file.$$"
                mv -f "$state/flake/.$file.$$" "$state/flake/$file"
            done
            nix develop "path:$state/flake" --no-write-lock-file --profile "$state/profile" -c true >&2
        fi
        exec nix develop "${nix_options[@]}" "$state/profile" --inputs-from "path:$state/flake" --option flake-registry "" -c "$@"
    else
        # Already in a nix shell, run command directly
        exec "$@"
    fi
else
    # Nix is not installed, run command directly
    exec "$@"
fi
