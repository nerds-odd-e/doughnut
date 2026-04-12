---
name: generate-api-client
description: >-
  Regenerate the frontend TypeScript API client from backend OpenAPI spec.
  Use after changing backend controller signatures or related data types.
  Triggers on: generateTypeScript, API regeneration, controller signature
  changed, OpenAPI lint failure, generated API code.
---

# Generate Frontend API Client

The frontend API client at `packages/generated/doughnut-backend-api` is auto-generated from the backend OpenAPI spec. **Never edit generated code directly.**

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

3. Check if the change affected frontend usage — run frontend tests:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test
```

4. Fix any broken frontend code that used the old API signatures.

## OpenAPI lint failures

If `pnpm openapi:lint` fails: **do not** edit `open_api_docs.yaml` (it is generated). Fix Java controllers, regenerate, then re-lint. Playbook details: `.cursor/rules/linting_formating.mdc` → **OpenAPI Linting**.
