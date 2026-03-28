# Phase 1 sub-plan: install + interactive greeting E2E

Sub-plan for **Phase 1** of `ongoing/cli-install-interactive-mode-e2e.md`. Each slice is **committable with CI green**, avoids **dead production code**, and follows `.cursor/rules/planning.mdc` (observable surfaces, one main failing test when driving implementation).

**Parent outcome:** Installed CLI with a TTY and **no** subcommand shows **`doughnut 0.1.0`** in **past CLI assistant messages** (first two Gherkin steps only; `/exit` lines stay commented until Phase 2).

**Progress:** Sub-phases **1.1–1.3** are **done**. **Next:** **1.4** (optional parent-plan pointer); then parent **Phase 2** (`/exit` + uncomment last two steps).

**Link:** Optional one-line pointer from `ongoing/cli-install-interactive-mode-e2e.md` Phase 1 to this file — not added yet (see 1.4).

---

## CI and local verify

- **Default CI** (`TAGS: not @ignore`, `e2e_test/config/ci.ts`) runs **`cli_install_and_run.feature`** including **Install and run the CLI in interactive mode** (no `@ignore` on that scenario).
- **Full install feature** (what CI exercises for this file):

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run \
  --spec e2e_test/features/cli/cli_install_and_run.feature \
  --config-file e2e_test/config/ci.ts
```

- **`pnpm cli:test`** — Vitest for the minimal interactive path (`runInteractive` + mock TTY stdin).

---

## Sub-phase 1.1 — PTY task + steps + assertion

**Deliverables**

- Cypress task **`runInstalledCliInteractive`**: spawn `node <installed doughnutPath>` with **no args**, **PTY** (`@lydell/node-pty`), merge env with `cliEnv()`, bounded capture then **kill** if still running (`e2e_test/config/cliE2ePluginTasks.ts`).
- Page object: **`installation().runInteractiveMode()`** → task → alias **`@cliInteractivePtyOutput`** (`e2e_test/start/pageObjects/cli/execution.ts`).
- Assertion: **`Then I should see {string} in past CLI assistant messages`** — ANSI-stripped transcript + diagnostics (`e2e_test/start/pageObjects/cli/outputAssertions.ts`).
- Step: **`When I run the installed doughnut command in interactive mode`** (`e2e_test/step_definitions/cli.ts`).

**Feature file:** `/exit` lines commented with `# Phase 2: …` (unchanged until Phase 2).

**Status:** **Done.**

---

## Sub-phase 1.2 — Product: minimal interactive session + Vitest

**Deliverables**

- **`cli/src/interactive.ts`:** TTY + no subcommand → log **`formatVersionOutput()`**, then wait on stdin **end**/**close** so the process stays alive until the PTY task kills it or stdin closes.
- **`cli/src/run.ts`:** **`await runInteractive()`**.
- **Vitest:** mock TTY **`Readable`**, **`runInteractive`**, assert version line via **`console.log`** (`cli/tests/index.test.ts`).

**Status:** **Done.**

---

## Sub-phase 1.3 — Enable in CI (remove `@ignore`)

**Deliverables**

- **`@ignore`** removed from **Install and run the CLI in interactive mode** only (`e2e_test/features/cli/cli_install_and_run.feature`).

**Status:** **Done.**

---

## Sub-phase 1.4 — Closeout

- Optionally add **one line** to `ongoing/cli-install-interactive-mode-e2e.md` under Phase 1: “Detailed slices: `ongoing/cli-install-phase1-subplan.md`.”
- When Phase 2 starts, use the parent plan; delete or trim this file when Phase 1 is fully closed and the team does not need the checklist.

**Status:** **Open** (optional doc touch-up only).

---

## Explicit non-goals (Phase 1)

- **`/exit`**, past user messages for slash input, shared step defs for generic “enter slash command” (Phase 2).
- Un-ignoring other **`@interactiveCLI`** features.
