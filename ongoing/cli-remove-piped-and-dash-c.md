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

Each phase should be **justified and tested inside the phase** (see `.cursor/rules/planning.mdc`). Suggested order:

### Phase 1 — Entry behavior: no `-c`, no `help` subcommand, interactive requires TTY

**User-visible outcome**

- Passing `-c` or `-c=` is an **error** (unknown flag or explicit “removed” message — pick one consistent style with other args).
- **`doughnut help`** removed; **`/help`** remains in interactive mode only.
- `doughnut` with **no subcommand** and **stdin not a TTY** exits **non-zero** with a short message (e.g. interactive shell requires a terminal).
- **`doughnut version`** (and `-v` / `--version`) and **`doughnut update`** still run without a TTY.

**Implementation sketch**

- `cli/src/run.ts`: remove `-c` handling; remove **`subcommand === 'help'`** branch; related imports (`isInteractiveOnlyCommand`, `INTERACTIVE_ONLY_REJECTION_MESSAGE` if only used for `-c`).
- `cli/src/help.ts`: remove or repurpose `isInteractiveOnlyCommand` / `INTERACTIVE_ONLY_REJECTION_MESSAGE` if nothing else needs them.
- `runInteractive`: if `!stdin.isTTY`, print error and `process.exit(1)` (or throw to `main` — match existing error style).

**Tests**

- `cli/tests/index.test.ts`: **delete** `-c` examples; add/adjust tests for “no TTY + no subcommand” failure, “`-c` rejected/unknown”, and **no `run(['help'])` success path** (or assert `help` subcommand is gone).
- `cli/tests/help.test.ts`: **do not** replace `-c` with `doughnut help`. Drop non-TTY help entry tests; keep or extend **TTY / `runInteractive`** coverage for **`/help`** content if not already redundant with `interactiveTty*` / E2E.

### Phase 2 — Delete piped interactive stack

**User-visible outcome**

- No user-facing change beyond phase 1 (piped interactive already subsumed by “require TTY”).

**Implementation sketch**

- Delete `cli/src/adapters/pipedAdapter.ts` and **`runPiped` / `buildPipedDeps`** from `interactive.ts`.
- Remove imports and comments that refer to “piped adapter” / “piped mode” in `renderer.ts`, `interactiveTtyStdout.ts`, etc., where they are no longer accurate.
- **`processInput`:** after TTY-only audit, remove **`parseRecallPipedYesNo` / `RECALL_PIPED_YES_NO_REPROMPT`** usage if **unreachable** when `interactiveUi === true` and no other caller exists; otherwise narrow naming to what remains.
- `defaultOutput` / `writeFullRedraw` paths in `processInput`: trim if **only** served piped layout; keep whatever TTY `OutputAdapter` still relies on.

**Tests (simplify aggressively)**

- **Remove** `cli/tests/interactive/interactivePiped.test.ts` and **`runPipedInteractive`** (and trim **`interactiveTestHelpers.ts`** to only what TTY tests still need).
- **`interactiveExitFarewell.test.ts`:** drop piped sections or fold into TTY if one case remains valuable.
- **`cli/tests/interactive/processInput.test.ts`:** **shrink** — delete bulk “console contract” scenarios that duplicated piped / `-c`; keep **only** what still guards real TTY-driven behavior **or** move remaining value into **`runInteractive` + mock TTY** so there is **one** primary style.
- **`cli/tests/recallYesNo.test.ts`:** drop or replace if `parseRecallPipedYesNo` is removed.
- **`cli/tests/interactiveFetchWait.test.ts`:** re-home or delete cases that only served non-TTY `processInput`.

### Phase 3 — E2E: features, steps, page objects, tasks (minimize glue)

**User-visible outcome**

- No `cli_non_interactive_mode.feature`.
- **`cli_recall.feature`:** token setup uses **interactive PTY** (e.g. `/add-access-token` with saved token + assertions on **history output** / **Current guidance**), not `-c` or piped spawn.
- **`cli_access_token.feature`** and **`cli_gmail.feature`:** same **@interactiveCLI** pattern as recall.
- Install / **version** / **update** scenarios keep **plain spawn** (no PTY) where they only need stdout — see `cli_install_and_run.feature`.

**Implementation sketch**

- **Delete** `e2e_test/features/cli/cli_non_interactive_mode.feature`.
- **`cli_recall.feature`:** rewrite Background / steps that used **`doughnut -c "/add-access-token"`** to **start interactive CLI** + **enter slash command** + token flow (reuse access-token page objects / steps to avoid duplication).
- **Rewrite** `cli_access_token.feature` and `cli_gmail.feature` with **`@interactiveCLI`**; OAuth simulation must work on PTY (extend **`cliPtyRunner` / plugin tasks** if needed).
- **`e2e_test/step_definitions/cli.ts`:** **delete** all steps that only exist for `-c` / `doughnut -c …`; keep the smallest set of **interactive** + **subcommand** steps.
- **`e2e_test/start/pageObjects/cli/execution.ts`:** remove **`nonInteractive().runWithCommand`**; remove **`runWithInput`** / **`runCliDirectWithInput`** paths if unused. **`accessToken()`** → PTY-only helpers (or inline one pattern).
- **`cliE2ePluginTasks.ts` / `cliE2eRepo.ts`:** remove **`runCliDirectWithInput`** and any spawn helper that pipes stdin into the full CLI.
- **`outputAssertions.ts` / `cliSectionParser.ts`:** update copy; **plain stdout** assertions apply only to **`version` / `update`** (and similar), not `help`.

**Test simplification goal**

- One obvious way to run “interactive” scenarios; **no** parallel “non-interactive output” steps for slash commands. Rename **“non-interactive output”** → something accurate (e.g. **subcommand stdout**) if the section is only for `version` / `update`.

### Phase 4 — Docs and stray references

- **`.cursor/rules/cli.mdc`**: terminology for TTY-only interactive; no `-c` / piped; **remove `help` subcommand** from docs; “Adding new commands”: verify with **`pnpm cli`** → **`/help`**, not `-c`.
- **`CLAUDE.md`**: same.
- **`ongoing/cli-osc-test-optimization.md`**: delete or rewrite piped / `-c` bullets.
- **`ongoing/cli-modal-architecture.md`**: trim piped sections that contradict TTY-only interactive.

## Verification commands (when executing)

- `CURSOR_DEV=true nix develop -c pnpm cli:test`
- `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_access_token.feature` (and gmail, recall, install as touched)
- `pnpm cli:lint` / format as usual

## Risks / open questions

- **Gmail OAuth simulation** today may use **`runCliDirectWithInput`**. PTY path must preserve **the same env + callback simulation**.
- **Recall `processInput` branches** using **`parseRecallPipedYesNo`**: confirm **TTY-only** recall never needs those line-based paths before deletion.
- **External scripts** using **`doughnut -c`** or **`doughnut help`** — intentional breaking change; document in release notes when shipping, not in this `ongoing/` file.
