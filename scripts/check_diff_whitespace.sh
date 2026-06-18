#!/usr/bin/env bash

set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

git diff --check "$@" -- . \
	":(exclude)open_api_docs.yaml" \
	":(exclude)packages/generated/doughnut-backend-api/**"
