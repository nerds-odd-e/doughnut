#!/usr/bin/env bash

set -eo pipefail

export savedFolder=/tmp/openapi-backend
export targetFolder=packages/generated-backend

rm -rf "$savedFolder"
mkdir -p "$savedFolder"
cp -r "$targetFolder" "$savedFolder"

pnpm generateTypeScript

if diff -r "$targetFolder" "$savedFolder/generated-backend"; then
  echo "The generated typescript interfaces are up-to-date"
else
  echo "The generated typescript interfaces are out of date"
  echo "Please run 'pnpm generateTypeScript'."
  exit 1
fi
