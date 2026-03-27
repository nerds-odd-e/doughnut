# CLI interactive strip-down (plan only — do not execute from this doc)

## Goal

- **Single active CLI E2E feature:** `e2e_test/features/cli/cli_install_and_run.feature` (both scenarios stay runnable; no `@ignore` on them).
- **All other** `e2e_test/features/cli/*.feature` files **remain on disk**; scenarios are progressively tagged **`@ignore`** until every scenario in those files is ignored. **Do not delete** those feature files at the end.
- **Remove** interactive product code and tests that exist only for the removed scenarios, while **keeping** the **Ink** dependency in the project (bundle may retain a minimal Ink surface or a tiny placeholder — decide during implementation).
- **Gmail exception:** keep **low-level** Gmail implementation (HTTP/OAuth/token plumbing, non-UI modules) and their **unit tests**; remove **Gmail UI** (Ink / TTY paths for `/add gmail`, `/last email`, etc.). Gmail E2E in `cli_gmail.feature` is still **ignored** like the other non-install features.

## Non-goals (for this cleanup)

- Rewriting `cli_install_and_run` assertions or changing install/update/version semantics unless required to keep the feature green.
- Removing Ink from `package.json` / bundle aliases.

## CI / tags

- Repo already excludes `@ignore` in `e2e_test/config/ci.ts` (`TAGS: 'not @ignore'`). Tag stripped scenarios with **`@ignore`** so they skip CI like other ignored features (same tag as elsewhere in the suite).

## Retained E2E contract (what must keep working)

`cli_install_and_run.feature` depends on:

- Install task + `runInstalledCli` (PTY path with `input: 'exit\n'` for the first scenario; spawn without stdin for `version` / `update`).
- Transcript parsing used by **`past CLI assistant messages`** / **`past user messages`** / **`non-interactive output`** (`e2e_test/start/pageObjects/cli/outputAssertions.ts` and tasks in `cliE2ePluginTasks.ts` / `cliPtyRunner.ts`).

Any refactor must keep **`pnpm cypress run --spec e2e_test/features/cli/cli_install_and_run.feature`** (with Nix wrapper per project rules) green after each logical batch.

## Workflow (repeat inside every E2E-driven sub-phase)

### A — Per scenario (E2E side)

1. **Remove** one scenario from “active” execution: add **`@ignore`** to the **scenario you are taking out next** (work **from the bottom of the feature file upward** so the “last” scenario in the file is ignored first; if a Rule/Outline makes order ambiguous, treat **last in file order** as the next target).
2. **Prune steps:** if a step is **not** referenced by any **non-`@ignore`** scenario **in the whole repo**, remove its step definition and then remove dead page objects / tasks / types it was the sole user of.
3. **Run** the relevant E2E slice (at minimum `cli_install_and_run.feature`; plus any features that still have non-ignored scenarios).
4. **Stop** — developer review and commit.

### B — Per scenario (production + Vitest side)

1. **Remove** product code that **only** supported the scenario just ignored (be aggressive).
2. **Remove** unit tests for deleted code, following **section “Unit test removal”** below (respect Gmail reservation).
3. **`pnpm cli:test`** (with Nix wrapper) green.
4. **Stop** — developer review and commit.

### C — Repeat A+B until the **entire** feature has only `@ignore` scenarios. **Keep the `.feature` file.**

---

## Unit test removal (when not driven by a single E2E scenario)

Use this for **leftover** Vitest groups after large deletes, or when a test file no longer has a clear product owner.

1. Work **one test group / file** at a time (a `describe` or whole file — pick the smallest coherent unit).
2. Delete helpers, **code under test**, and exclusive dependencies where possible.
3. Collapse **empty or redundant** abstraction layers once behavior is gone.
4. If a test was meant to lock **meaningful external, user-centric** behavior that **temporarily** has no home, replace the body with **`skip`** (empty body) rather than silently dropping the intent; otherwise **delete** the test.
5. **Stop** — developer review and commit.

---

## Top-level phases — Part 1: by E2E feature file

Recommended **order** (heavy / shared steps first, then independent shells):

1. **`cli_recall.feature`** — most scenarios and recall-specific steps (`answerToPrompt`, down-arrow MCQ, `recall session was stopped`, etc.).
2. **`cli_access_token.feature`** — shared steps with recall (`addSavedTokenInteractive`, token list); easier to strip after recall is fully ignored.
3. **`cli_gmail.feature`** — two scenarios; remove Gmail **UI** while keeping low-level modules + `gmail.test.ts` (and any other UT tied to non-UI Gmail).
4. **`cli_interactive_mode.feature`** — generic TTY assertions (`Current guidance`, input box border, plain line, `/help`, `exit`).

### Part 1.1 — Feature: `cli_recall.feature`

Work **bottom-to-top** (scenario list as in file):

| Sub-phase | Scenario (summary) |
|-----------|-------------------|
| ~~1.1.1~~ | ~~Recall spelling — type correct spelling and see success~~ **done** (`@ignore` + CLI spelling path removed) |
| 1.1.x | Recall MCQ — contest and regenerate before answering |
| 1.1.x | Recall MCQ — down arrow and Enter to select |
| 1.1.x | Recall MCQ — ESC cancels with y/n confirmation |
| 1.1.x | Recall MCQ — choose correct answer and see success |
| 1.1.x | Recall session — complete all due notes, summary, load more |
| 1.1.x | Recall Just Review |
| 1.1.x | Recall status shows count |

For each row: **A** then **B** (commit stops). Shared **Background** steps become deletable only when **no** non-ignored scenario in this file (or others) still uses them.

### Part 1.2 — Feature: `cli_access_token.feature`

Bottom-to-top:

| Sub-phase | Scenario (summary) |
|-----------|-------------------|
| 1.2.x | Create access token via CLI |
| 1.2.x | Another key cancels remove-access-token selection |
| 1.2.x | Scenario Outline: Remove access token (both examples — can be one E2E sub-phase if both ignored together, still follow A/B commits) |
| 1.2.x | Add invalid access token |
| 1.2.x | Add access token and list it |

### Part 1.3 — Feature: `cli_gmail.feature`

Bottom-to-top:

| Sub-phase | Scenario (summary) |
|-----------|-------------------|
| 1.3.x | last email shows subject when account is configured |
| 1.3.x | add gmail adds account when OAuth callback is simulated |

E2E: tag **`@ignore`**, remove Gmail-specific step defs / page object helpers / hooks tags **`@interactiveCLIGmail*`** / env builders **only when unused**. Product: delete **Gmail Ink/TTY UI**; **retain** low-level Gmail services + **unit tests** (`cli/tests/gmail.test.ts` and direct dependencies that are not UI).

### Part 1.4 — Feature: `cli_interactive_mode.feature`

Bottom-to-top:

| Sub-phase | Scenario (summary) |
|-----------|-------------------|
| 1.4.x | exit ends the session after Bye |
| 1.4.x | /help lists subcommands and interactive commands |
| 1.4.x | After /help, consecutive Enter on empty input keeps a normal input box |
| 1.4.x | TTY interactive responds "Not supported" to a plain line |

After this file is fully **`@ignore`**, verify **`cli_install_and_run`** still passes; implement the **minimal** interactive path it needs (version banner, `exit`, transcript layout) without restoring full `/help` or recall behavior.

### Part 1.5 — E2E harness cleanup (once all non-install CLI features are fully ignored)

Single or few sub-phases (each with commit stop):

- Remove **`@interactiveCLI`** Before/After hooks and **long-lived** PTY session tasks if nothing references them.
- Prune **`InteractiveCliPtyKeystroke`** / `cliPtyRunner` / `interactiveCliPtyTypes` **only** if no step and no `runInstalledCli` path needs them (install may keep a **narrower** PTY helper).
- Trim **`e2e_test/step_definitions/cli.ts`** and **`e2e_test/start/pageObjects/cli/*`** to what **`cli_install_and_run`** + shared backend steps still need.
- Remove unused mock Gmail **Given** steps from `cli.ts` if no feature references them.

---

## Top-level phases — Part 2: Vitest groups still present but not justified by `cli_install_and_run`

Run these **after** Part 1 is complete (or when a group is discovered orphaned mid-way). Order by **safe deletion** (no imports left):

| Phase | Likely target (confirm at execution time) |
|-------|-------------------------------------------|
| 2.1 | Entire `cli/tests/interactive/` tree if any file remains |
| 2.2 | Recall-focused tests: `recall*.test.ts`, `recallYesNo.test.ts`, `recallMcq*.test.ts`, `recallNextTestShapes.ts`, `recallPromptFixtures.ts` |
| 2.3 | Token/list/shell UX: `accessToken.test.ts`, `selectListInteraction.test.ts`, `listDisplay.test.ts`, `interactiveCommandInput.test.ts`, `userInputHistoryFile.test.ts`, `inputHistoryMask.test.ts`, `shell/pastMessagesModel.test.ts` |
| 2.4 | TTY/Ink-heavy: `mainCommandLineInkTyping.test.ts`, `interactiveFetchWait.test.ts`, `ttyWriteSimulation.ts` (if only tests) |
| 2.5 | Slash/help if product no longer exposes them: `slashCompletion.test.ts`, `help.test.ts` |
| 2.6 | `processInput.test.ts` — only if `processInput` / slash engine removed or reduced to a stub |
| 2.7 | Renderer/layout: `renderer.test.ts`, `interactive/interactiveRendering.test.ts` — keep only if minimal install shell still needs assertions; else delete per unit-test-removal rules |
| 2.8 | `markdown.test.ts`, `sdkHttpErrorClassification.test.ts`, `index.test.ts` — **keep** if still required for `version`/`update`/entry; otherwise trim |
| 2.9 | **Reserved:** `gmail.test.ts` — **do not remove** without explicit change to the Gmail exception |
| 2.10 | **Keep:** `version.test.ts`, `update.test.ts` — align with install feature; extend only if behavior changes |
| 2.11 | Config hygiene: `cli/vitest.config.ts`, `stryker.conf.mjs` / `vitest.stryker.config.ts` — drop includes for deleted tests |

Each row: apply **Unit test removal** workflow (section above); one commit stop per group unless trivially empty.

---

## Verification commands (execution time)

- E2E: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_install_and_run.feature`
- CLI unit: `CURSOR_DEV=true nix develop -c pnpm cli:test`
- Optional broader regression if touching shared Cypress config: run full `e2e_test` only when warranted.

---

## Docs / rules (after implementation, not in first commit)

- Update `.cursor/rules/cli.mdc` (and any E2E rule sections) to match the **new** CLI surface so future agents do not reintroduce removed terminology.
- This file **`ongoing/cli-interactive-strip.md`** should be deleted or archived when the cleanup is done.
