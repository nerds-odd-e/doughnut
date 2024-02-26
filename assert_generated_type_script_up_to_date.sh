#!/usr/bin/env bash

set -eo pipefail

export savedFolder=/tmp/openapi-backend
export targetFolder=frontend/src/generated/backend

rm -rf "$savedFolder"
mkdir -p "$savedFolder"

pnpm generateTypeScript

if diff -r "$targetFolder" "$savedFolder"; then
  echo "The generated typescript interfaces are up-to-date"
else
  echo "The generated typescript interfaces are out of date"
  echo "Please run 'pnpm generateTypeScript'."
  exit 1
fi
