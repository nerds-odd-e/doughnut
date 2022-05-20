#!/usr/bin/env bash

set -eo pipefail

export savedFile=/tmp/rest.d.ts
export targetFile=frontend/src/@types/generated/rest.d.ts
cp $targetFile $savedFile
backend/gradlew -p backend generateTypeScript
if cmp "$targetFile" "$savedFile"; then
  echo "The generated typescript interfaces are up-to-date"
else
  echo "The generated typescript interfaces are out of date"
  echo "Please run './gradlew generateTypeScript' in backend."
  exit 1
fi
