---
name: mutation-testing
description: >-
  Run and interpret Stryker mutation testing results. Use ONLY when the
  developer explicitly asks for mutation testing or when a plan phase
  includes it. Do NOT use proactively.
---

<objective>
Run Stryker mutation tests and interpret survived mutants to improve real test
coverage — without adding shallow structure-pinning tests.

Purpose: Validate that unit tests exercise module logic after extract/refactor,
or compare integration coverage vs mutation kill rate on pure/narrow modules.

Output: Mutation run results + interpretation + summary ending with
`## MUTATION TEST COMPLETE`.
</objective>

<context>
**Use only when:** the developer explicitly asks for mutation testing, or a
plan phase includes it. **Do not use proactively.**

**Current scope:** `cli/` only. Later: reuse in `frontend/`, `mcp-server/`.

**Git does not use the Nix prefix.** All other repo tooling does:
`CURSOR_DEV=true nix develop -c …`

**Policy on survived mutants:** A survived mutant means no test executes that
branch — **or** the code is dead/unreachable. **Do not automatically add
tests.** First consider delete dead path, merge redundant logic, or prove the
code is still required from product/domain behavior.

**Do not** add shallow tests that reach into inner structure, call
non-entry-point helpers, or assert on internal state just to flip Stryker green.

**Do** add or extend coverage by driving a **high-level entry point**
(controller, mounted page, `runInteractive`, real CLI). Assert only what a user
or integrator can observe: HTTP response, CLI stdout, DOM, black-box
inputs/outputs.

**Pure helpers** (inputs → result, no hidden globals) may keep direct-import
black-box tests only when that API is the intentional stable contract.

If a good observable test is hard to compose, **ask the developer** whether the
mutated code deserves to exist.
</context>

<process>

<step name="run_mutation">
```bash
CURSOR_DEV=true nix develop -c bash -c 'cd cli && pnpm test:mutation'
```
</step>

<step name="interpret_survivors">
For each survived mutant, apply the policy above: delete, merge, prove
necessity, or add observable entry-point coverage — never shallow
structure-pinning tests.
</step>

<step name="package_setup_reference">
| Piece | Role |
|-------|------|
| `devDependencies` | `@stryker-mutator/core`, `@stryker-mutator/vitest-runner` |
| `stryker.conf.mjs` | Must set `plugins: ['@stryker-mutator/vitest-runner']` |
| `vitest.stryker.config.ts` | Dedicated Vitest config for mutation runs |
| `.gitignore` | `.stryker-tmp/`, `reports/mutation/`, `stryker-setup-*.js` |
</step>

<step name="known_pitfalls">
- **`vitest.related`** — Vitest may skip tests that reach mutated code
  indirectly. Fix: `vitest: { related: false }` in `stryker.conf.mjs`.
- **`mergeConfig` + `test.include`** — Can union includes so Stryker runs too
  many tests. Fix: standalone `vitest.stryker.config.ts` with only the
  relevant tests.
- **`coverageAnalysis: 'perTest'`** — Can show no effective coverage on ESM
  setups. `coverageAnalysis: 'off'` with a small test set is acceptable.
</step>

<step name="extend_to_subproject">
1. Add devDependencies and `stryker.conf.mjs` (adjust `mutate`,
   `vitest.configFile`).
2. Point `vitest.stryker.config.ts` at only tests that import mutated sources.
3. Add ignore block to that package's `.gitignore`.
4. Wire a root/package script with the Nix prefix.
</step>

</process>

<success_criteria>
- Mutation run executed with Nix prefix (`cli/` scope unless extending)
- Survived mutants interpreted per policy (no auto shallow tests)
- Developer consulted when observable coverage is hard to compose
- Final output includes `## MUTATION TEST COMPLETE`
</success_criteria>

<output>
Report mutation results, survived-mutant decisions, and any recommended
follow-ups, then:

```
## MUTATION TEST COMPLETE
```
</output>

<out_of_scope>
- Do not run proactively without explicit developer or plan request.
- Do not add tests that only pin internal structure to kill mutants.
- Do not extend beyond `cli/` unless following `extend_to_subproject`.
</out_of_scope>
