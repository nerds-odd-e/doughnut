# Remove piped stdin and `-c` from doughnut-cli

Informal requirement; delete or shrink when done. **Do not treat this file as shipped user documentation.**

## Goal

- Drop **`-c` / `-c=<cmd>`** entirely from the CLI entrypoint.
- Drop **all piped stdin** into the interactive shell: no `runPiped`, no `runCliDirectWithInput` driving the full CLI.
- **Simplify code that previously supported both piped and TTY** — one interactive path (**TTY only**), fewer branches and adapters; not “support both” going forward.
- **E2E:** remove `cli_non_interactive_mode.feature`; drive **access token**, **Gmail**, and **recall token setup** via **`@interactiveCLI`** (PTY). **`cli_recall.feature`:** switch **token setup** from `-c` / piped to **interactive** (same style as other recall scenarios).
- **Tests:** when `-c` and piped paths go away, **delete or merge** steps, tasks, and Vitest files aggressively — **minimum** surface that still proves behavior (prefer one PTY + subcommand pattern over parallel “non-interactive” helpers).
- **OSC:** `INTERACTIVE_INPUT_READY_OSC` is **TTY automation only**. After this work, confirm there is **no** OSC path that existed solely for removed modes; **no product change** expected to TTY OSC unless audit finds dead code.

## Non-goals (unless a later phase explicitly adds them)

- Replacing piped stdin with another headless automation mode (new flag, etc.).
- Keeping **`doughnut help`** as a **non-TTY subcommand**. Help is **interactive only** (`/help` in the TTY shell).

## Non-TTY entry (explicit)

Still allowed **without a TTY** (scripts / CI / install checks):

- **`doughnut version`**, **`--version`**, **`-v`**
- **`doughnut update`**

Not allowed without TTY:

- Interactive shell (use a terminal).
- **`doughnut help`** — **remove** the `help` subcommand from `run.ts`; users use **`/help`** after starting the TTY app.

## Current architecture (snapshot)

- `cli/src/run.ts`: `-c` → `processInput` + `exit`; else subcommands (`version`, `update`, `help`, …); else `runInteractive()`.
- `cli/src/interactive.ts`: `runInteractive` branches on `stdin.isTTY` → `runTTY` vs `runPiped` (`cli/src/adapters/pipedAdapter.ts`).
- `processInput` is the **shared command engine**; TTY calls it with `interactiveUi: true` and a TTY `OutputAdapter`. Piped / `-c` used **default console** adapter and `interactiveUi` as wired by caller.
- Recall **line-based** y/n (`parseRecallPipedYesNo` in `cli/src/interactions/recallYesNo.ts`) lives **inside `processInput`** for load-more and yes/no recall answers — historically aligned with piped / `-c`. TTY recall confirmations are **Ink**; need an **audit** after piped removal: either delete unreachable branches or prove a TTY path still needs them.

## Phased delivery (scenario-first, order by value)

### Phase discipline — every phase must finish green

**Do not merge a phase** until **all** of the following pass (same bar for each phase):

1. **Unit tests:** at minimum **`CURSOR_DEV=true nix develop -c pnpm cli:test`**. If the team’s merge bar is repo-wide, run **`pnpm verify`** (or **`pnpm test`**) so **backend + frontend + CLI unit tests** stay green, not only `cli/`.
2. **Full E2E suite** (same scope CI uses for Cypress): e.g. **`CURSOR_DEV=true nix develop -c pnpm cy:run`** (see `package.json` / CI if the command changes).
3. **Lint/format** for touched packages as usual (`pnpm cli:lint`, `pnpm cy:lint`, etc.).

**Why reorder phases below:** Removing `-c` or piped **before** E2E stops using them would **break the full E2E run**. So **migrate Cypress CLI features and tasks first** (product still supports old entry points), then **remove** `-c` / `help` subcommand / non-TTY interactive, then **delete** the piped implementation and shrink Vitest — each step leaves **unit + full E2E** green.

---

### Phase 1 — E2E only: PTY for slash commands; no reliance on `-c` or piped spawn

**Product change:** none required (CLI may still accept `-c` and piped stdin for this phase).

**User-visible outcome (tests only):** Scenarios prove the same behaviors through **`@interactiveCLI`** and subcommand spawn where appropriate.

**Implementation sketch**

- **Delete** `e2e_test/features/cli/cli_non_interactive_mode.feature` (or replace with a single **subcommand** scenario if something unique must remain — prefer moving **version** assertion next to `cli_install_and_run` if needed).
- **`cli_recall.feature`**, **`cli_access_token.feature`**, **`cli_gmail.feature`:** rewrite so **token setup**, Gmail, and access-token flows use **interactive PTY** only (`startInteractiveCli`, slash commands, **history output** / **Current guidance** assertions).
- **Remove** Cypress steps/tasks/page objects that only exist for **`doughnut -c …`** or **`runCliDirectWithInput`** / piped full-CLI spawn **once nothing references them**.
- **`outputAssertions` / `cliSectionParser`:** adjust naming/copy toward **subcommand stdout** vs interactive sections as needed; keep assertions valid for migrated scenarios.
- **Gmail:** OAuth simulation must work on the PTY path (extend **`cliPtyRunner` / plugin tasks** if required).

**Unit tests:** unchanged in intent — full **`pnpm cli:test`** must still pass.

**Phase gate:** `pnpm cli:test` + **full** `pnpm cy:run` (or equivalent) green.

---

### Phase 2 — Entry behavior: remove `-c`, remove `help` subcommand, interactive requires TTY

**User-visible outcome**

- Passing `-c` or `-c=` is an **error**.
- **`doughnut help`** removed; **`/help`** remains in the TTY shell only.
- `doughnut` with **no subcommand** and **stdin not a TTY** exits **non-zero** (clear message).
- **`doughnut version`** / **`-v`** / **`--version`** and **`doughnut update`** still work without a TTY.

**Implementation sketch**

- `cli/src/run.ts`: remove `-c`; remove **`help` subcommand** branch; clean related **`help.ts`** exports if unused (`isInteractiveOnlyCommand`, `INTERACTIVE_ONLY_REJECTION_MESSAGE`, etc.).
- `runInteractive`: if `!stdin.isTTY`, error + non-zero exit.

**Unit tests**

- `cli/tests/index.test.ts`: drop `-c` coverage; add **`-c` rejected**, **no TTY + no subcommand** failure, **`help` subcommand gone**.
- `cli/tests/help.test.ts`: remove non-TTY **`doughnut help`** / **`-c` /help** style tests; rely on **TTY / `runInteractive`** (or existing `interactiveTty*`) for **`/help`** content.

**Phase gate:** `pnpm cli:test` + **full** E2E green (Phase 1 already removed E2E dependence on `-c` / `help` subcommand).

---

### Phase 3 — Delete piped interactive stack; shrink CLI unit tests

**User-visible outcome**

- No piped line-by-line interactive shell; **TTY only** for `runInteractive`.

**Implementation sketch**

- Delete **`pipedAdapter.ts`**, **`runPiped`**, **`buildPipedDeps`**; **`runInteractive`** always **`runTTY`** after the non-TTY guard.
- **`processInput`:** audit and remove **`parseRecallPipedYesNo`** / reprompt / dead **`defaultOutput`** paths if TTY-only; trim **`renderer` / `interactiveTtyStdout`** comments that mention piped.
- **Dead E2E:** confirm **`runCliDirectWithInput`** and any piped spawn helpers are **gone** (should already be unused after Phase 1); delete from **`cliE2ePluginTasks`** / **`cliE2eRepo`** if still present.

**Unit tests (simplify aggressively, same commit)**

- Delete **`interactivePiped.test.ts`**, **`runPipedInteractive`** and other piped-only helpers; fold any unique assertion into **mock TTY** tests if worth keeping.
- Shrink **`processInput.test.ts`**, **`interactiveExitFarewell.test.ts`**, **`interactiveFetchWait.test.ts`**, **`recallYesNo.test.ts`** per audit — **no intentionally failing tests** left at phase end.

**Phase gate:** `pnpm cli:test` + **full** E2E green.

---

### Phase 4 — Docs and `ongoing/` cleanup

- **`.cursor/rules/cli.mdc`**, **`CLAUDE.md`**, **`ongoing/cli-osc-test-optimization.md`**, **`ongoing/cli-modal-architecture.md`**: align with TTY-only interactive, no `-c` / piped / `doughnut help`.

**Phase gate:** same as **Phase discipline** — unit bar (`pnpm cli:test` or repo `pnpm verify` / `pnpm test` per team rule) + **full** E2E green (sanity run; docs should not change code paths).

## Verification commands (when executing)

Per **Phase discipline** above — each phase ends with:

- `CURSOR_DEV=true nix develop -c pnpm cli:test` (and **`pnpm verify`** or **`pnpm test`** if that is the required unit-test bar for merge)
- `CURSOR_DEV=true nix develop -c pnpm cy:run` (full E2E; adjust if CI uses a different target)
- `pnpm cli:lint` / `pnpm cy:lint` (and other linters) as needed for touched files

## Risks / open questions

- **Gmail OAuth simulation** today may use **`runCliDirectWithInput`**. PTY path must preserve **the same env + callback simulation**.
- **Recall `processInput` branches** using **`parseRecallPipedYesNo`**: confirm **TTY-only** recall never needs those line-based paths before deletion.
- **External scripts** using **`doughnut -c`** or **`doughnut help`** — intentional breaking change; document in release notes when shipping, not in this `ongoing/` file.
