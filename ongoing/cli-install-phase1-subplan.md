# Phase 1 sub-plan: install + interactive greeting E2E

Sub-plan for **Phase 1** of `ongoing/cli-install-interactive-mode-e2e.md`. Each slice is **committable with CI green**, avoids **dead production code**, and follows `.cursor/rules/planning.mdc` (observable surfaces, one main failing test when driving implementation).

**Parent outcome:** Installed CLI with a TTY and **no** subcommand shows **`doughnut 0.1.0`** in **past CLI assistant messages** (first two Gherkin steps only; `/exit` steps stay commented until Phase 2).

**Link:** Update the parent plan briefly when this sub-plan supersedes part of its Phase 1 checklist (optional one-line pointer).

---

## CI vs red feedback

- **Default CI** uses `TAGS: not @ignore` (`e2e_test/config/ci.ts`). The interactive install scenario stays **`@ignore`** until the **product + E2E** slice that should go green in CI.
- **Local red test** (wrong/missing assistant message): run only the ignored interactive install scenario:

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run \
  --config-file e2e_test/config/ci.ts \
  --spec e2e_test/features/cli/cli_install_and_run.feature \
  --env TAGS='@bundleCliE2eInstall and @ignore'
```

Expect **one** scenario (interactive install). Failure should say **why** the PTY transcript did not contain the expected substring (not a generic Cypress error).

---

## Sub-phase 1.1 — PTY task + steps + assertion (scenario still `@ignore`)

**Deliverables**

- Cypress task **`runInstalledCliInteractive`**: spawn `node <installed doughnutPath>` with **no args**, **PTY** (**`@lydell/node-pty`** — same API as `node-pty`, platform prebuilds via optional deps), merge env with `cliEnv()` (`e2e_test/config/cliEnv.ts`), capture output for a bounded time or until process exit, then **kill** if still running so Cypress does not hang.
- Page object: **`installation().runInteractiveMode()`** → task → alias **`@cliInteractivePtyOutput`**.
- Centralized assertion: **`Then I should see {string} in past CLI assistant messages`** — search **ANSI-stripped** transcript; on miss, throw with **expected substring**, **length**, and a **truncated preview** (empty transcript called out explicitly).
- Step: **`When I run the installed doughnut command in interactive mode`** → `runInteractiveMode()` only.

**Feature file**

- Comment out the **`/exit`** lines with `# Phase 2: …` (per parent plan).
- Keep **`@ignore`** on the scenario.

**Verify:** Run the local command above; should **fail** because the CLI still does not emit the greeting in the PTY transcript (or exits with no assistant text). Failure message must be **clear** (assertion layer, not opaque).

**Done when:** CI green; local tagged run fails for the **right** reason.

---

## Sub-phase 1.2 — Product: minimal interactive session + Vitest

**Deliverables**

- **`cli/src/interactive.ts`**: with a TTY and no subcommand path already taken, start a **minimal** session that does **not** exit immediately; emit an initial assistant-visible line that includes **`formatVersionOutput()`** from `cli/src/commands/version.ts` (same string as `doughnut version`).
- **Vitest:** mock TTY stdin, drive **`runInteractive`** (or the same entry `run` uses), assert the version string appears in captured output — **observable surface only**.

**Verify:** `pnpm cli:test` green; local Cypress command from 1.1 **passes** (transcript contains `doughnut 0.1.0`).

**Done when:** Red → green on the ignored scenario; still **`@ignore`** if you want one more commit before CI expands (or proceed to 1.3).

---

## Sub-phase 1.3 — Enable in CI (remove `@ignore`)

**Deliverables**

- Remove **`@ignore`** from **Install and run the CLI in interactive mode** only (first two steps + commented `/exit` lines unchanged).

**Verify**

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run \
  --spec e2e_test/features/cli/cli_install_and_run.feature \
  --config-file e2e_test/config/ci.ts
```

**Done when:** Full install feature green under default CI tags; no dead CLI code; Vitest from 1.2 still green.

---

## Sub-phase 1.4 — Closeout

- Optionally add **one line** to `ongoing/cli-install-interactive-mode-e2e.md` under Phase 1: “Detailed slices: `ongoing/cli-install-phase1-subplan.md`.”
- When Phase 2 starts, use the parent plan; delete or trim this file when Phase 1 is fully done and the team does not need the checklist.

---

## Explicit non-goals (Phase 1)

- **`/exit`**, past user messages for slash input, shared step defs for generic “enter slash command” (Phase 2).
- Un-ignoring other **`@interactiveCLI`** features.
