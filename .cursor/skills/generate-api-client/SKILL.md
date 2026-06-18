---
name: generate-api-client
description: >-
  Regenerate the frontend TypeScript API client from backend OpenAPI spec.
  Use after changing backend controller signatures or related data types.
  Triggers on: generateTypeScript, API regeneration, controller signature
  changed, OpenAPI lint failure, generated API code.
---

# Generate Frontend API Client

The frontend API client at `packages/generated/doughnut-backend-api` is auto-generated from the backend OpenAPI spec. **Never edit generated code directly.** This includes whitespace-only cleanup in `sdk.gen.ts`, `types.gen.ts`, and `open_api_docs.yaml`; if generated output changes, accept it or fix the generator path.

## When to regenerate

- A backend controller signature changed
- A backend data type used by any controller signature changed
- `pnpm openapi:lint` or `pnpm format:all` fails with OpenAPI validation errors

## Steps

1. Fix the **Java controllers** (not the generated files).

2. Regenerate:

```bash
CURSOR_DEV=true nix develop -c pnpm generateTypeScript
```

Do not run raw `git diff --check` as a generated-client cleanup gate. Use `scripts/check_diff_whitespace.sh`, which excludes generated API artifacts from manual whitespace fixes.

3. Check if the change affected frontend usage — run frontend tests:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test
```

4. Fix any broken frontend code that used the old API signatures.

## OpenAPI lint failures

If `pnpm openapi:lint` fails: **do not** edit `open_api_docs.yaml` (it is generated). Fix Java controllers, regenerate, then re-lint. Playbook details: `.cursor/rules/linting_formating.mdc` → **OpenAPI Linting**.
