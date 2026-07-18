---
name: generate-api-client
description: >-
  Regenerate the frontend TypeScript API client from backend OpenAPI spec.
  Use after changing backend controller signatures or related data types.
  Triggers on: generateTypeScript, API regeneration, controller signature
  changed, OpenAPI lint failure, generated API code.
---

<objective>
Regenerate the frontend TypeScript API client from the backend OpenAPI spec and
fix any broken frontend usage.

Purpose: Keep `packages/generated/doughnut-backend-api` in sync with Java
controllers — never hand-edit generated output.

Output: Regenerated client + green frontend tests + summary ending with
`## API CLIENT GENERATED`.
</objective>

<context>
The frontend API client at `packages/generated/doughnut-backend-api` is
auto-generated from the backend OpenAPI spec.

**Never edit generated code directly.** This includes whitespace-only cleanup in
`sdk.gen.ts`, `types.gen.ts`, and `open_api_docs.yaml`; if generated output
changes, accept it or fix the generator path.

**Git does not use the Nix prefix.** All other repo tooling does:
`CURSOR_DEV=true nix develop -c …`

Do not run raw `git diff --check` as a generated-client cleanup gate. Use
`scripts/check_diff_whitespace.sh`, which excludes generated API artifacts from
manual whitespace fixes.

**Regenerate when:**
- A backend controller signature changed
- A backend data type used by any controller signature changed
- `pnpm openapi:lint` or `pnpm format:all` fails with OpenAPI validation errors
</context>

<process>

<step name="fix_java_controllers">
Fix the **Java controllers** (not the generated files).
</step>

<step name="regenerate">
```bash
CURSOR_DEV=true nix develop -c pnpm generateTypeScript
```
</step>

<step name="verify_frontend">
Check if the change affected frontend usage — run frontend tests:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test
```

Fix any broken frontend code that used the old API signatures.
</step>

<step name="openapi_lint_failures">
If `pnpm openapi:lint` fails: **do not** edit `open_api_docs.yaml` (it is
generated). Fix Java controllers, regenerate, then re-lint. Playbook details:
`.cursor/rules/linting_formating.mdc` → **OpenAPI Linting**.
</step>

</process>

<success_criteria>
- Java controllers fixed at source (not generated files)
- `pnpm generateTypeScript` run with Nix prefix
- Frontend tests pass after signature changes
- OpenAPI lint passes (fix controllers + regenerate, never hand-edit YAML)
- Final output includes `## API CLIENT GENERATED`
</success_criteria>

<output>
Report a short summary to the caller, then the completion marker:

1. What triggered regeneration (controller/DTO change, lint failure, etc.).
2. Whether frontend call sites needed updates.
3. Tests run and confirmed passing.

```
## API CLIENT GENERATED
```
</output>

<out_of_scope>
- Do not hand-edit `sdk.gen.ts`, `types.gen.ts`, or `open_api_docs.yaml`.
- Do not use raw `git diff --check` for generated-client whitespace gates.
</out_of_scope>
