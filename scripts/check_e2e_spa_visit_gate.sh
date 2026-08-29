#!/usr/bin/env bash
#
# Fail if any e2e_test TypeScript file other than start/router.ts calls cy.visit(.
# Page objects and steps must go through that named SPA visit gate.

set -euo pipefail

E2E_DIR="${1:-e2e_test}"
E2E_DIR="${E2E_DIR%/}"
SPA_VISIT_GATE="start/router.ts"

if [[ ! -d "$E2E_DIR" ]]; then
	echo "ERROR: e2e directory not found: $E2E_DIR" >&2
	exit 1
fi

offenders=""
while IFS= read -r -d '' file; do
	relpath="${file#"$E2E_DIR"/}"
	if [[ "$relpath" == "$SPA_VISIT_GATE" ]]; then
		continue
	fi
	while IFS= read -r match; do
		[[ -n "$match" ]] || continue
		lineno="${match%%:*}"
		text="${match#*:}"
		if [[ "$text" =~ ^[[:space:]]*// ]]; then
			continue
		fi
		offenders+="${file}:${lineno}:${text}"$'\n'
	done < <(grep -n 'cy\.visit(' "$file" || true)
done < <(find "$E2E_DIR" -type f -name '*.ts' -print0 | sort -z)

if [[ -n "$offenders" ]]; then
	echo "ERROR: cy.visit( is only allowed in ${E2E_DIR}/${SPA_VISIT_GATE}" >&2
	printf '%s' "$offenders" >&2
	exit 1
fi

echo "OK: cy.visit( calls are only in ${E2E_DIR}/${SPA_VISIT_GATE}"
