---
name: mutation-testing
description: >-
  Run and interpret Stryker mutation testing results. Use ONLY when the
  developer explicitly asks for mutation testing or when a plan phase
  includes it. Do NOT use proactively.
---

# Mutation Testing (Stryker + Vitest)

**Current scope:** `cli/` only. Later: reuse in `frontend/`, `mcp-server/`.

## When to use

- Checking whether unit tests actually exercise a module's logic (after extract/refactor).
- Comparing integration-only coverage vs mutation kill rate on a pure/narrow module.

## Run

```bash
CURSOR_DEV=true nix develop -c bash -c 'cd cli && pnpm test:mutation'
```

## Interpreting survived mutants

A survived mutant means no test executes that branch — **or** the code is dead/unreachable. **Do not automatically add tests.** First consider:

1. **Delete** the dead path or unreachable code.
2. **Merge** redundant logic.
3. **Prove** the code is still required from product/domain behavior.

**Do not** add shallow tests that reach into inner structure, call non-entry-point helpers, or assert on internal state just to flip Stryker green.

**Do** add or extend coverage by driving a **high-level entry point** (controller, mounted page, `runInteractive`, real CLI). Assert only what a user or integrator can observe: HTTP response, CLI stdout, DOM, black-box inputs/outputs.

**Pure helpers** (inputs → result, no hidden globals) may keep direct-import black-box tests only when that API is the intentional stable contract.

If a good observable test is hard to compose, **ask the developer** whether the mutated code deserves to exist.

## Package setup

| Piece | Role |
|-------|------|
| `devDependencies` | `@stryker-mutator/core`, `@stryker-mutator/vitest-runner` |
| `stryker.conf.mjs` | Must set `plugins: ['@stryker-mutator/vitest-runner']` |
| `vitest.stryker.config.ts` | Dedicated Vitest config for mutation runs |
| `.gitignore` | `.stryker-tmp/`, `reports/mutation/`, `stryker-setup-*.js` |

## Known pitfalls

- **`vitest.related`** — Vitest may skip tests that reach mutated code indirectly. Fix: `vitest: { related: false }` in `stryker.conf.mjs`.
- **`mergeConfig` + `test.include`** — Can union includes so Stryker runs too many tests. Fix: standalone `vitest.stryker.config.ts` with only the relevant tests.
- **`coverageAnalysis: 'perTest'`** — Can show no effective coverage on ESM setups. `coverageAnalysis: 'off'` with a small test set is acceptable.

## Extending to another subproject

1. Add devDependencies and `stryker.conf.mjs` (adjust `mutate`, `vitest.configFile`).
2. Point `vitest.stryker.config.ts` at only tests that import mutated sources.
3. Add ignore block to that package's `.gitignore`.
4. Wire a root/package script with the Nix prefix.
