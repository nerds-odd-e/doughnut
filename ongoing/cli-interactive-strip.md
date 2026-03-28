# CLI interactive strip-down (plan only — do not execute from this doc)

## Current state (after Part 1)

- **Active CLI E2E:** `e2e_test/features/cli/cli_install_and_run.feature` — **version** and **update** only; **`runInstalledCli`** is **spawn** (no PTY / `node-pty`). Other `e2e_test/features/cli/*.feature` scenarios are **`@ignore`** but files stay on disk.
- **Ink** stays in the project; interactive surface was stripped from E2E and product as per completed work.
- **Gmail:** low-level modules + `cli/tests/gmail.test.ts` **retain**; Gmail **UI** / TTY paths removed; `cli_gmail.feature` remains ignored.

## Non-goals

- Rewriting `cli_install_and_run` assertions or install/update/version semantics unless required to keep the feature green.
- Removing Ink from `package.json` / bundle aliases.

## Retained E2E contract

`cli_install_and_run.feature` needs:

- Install task + **spawn** `runInstalledCli` for `version` / `update`.
- **`non-interactive output`** guard in `e2e_test/start/pageObjects/cli/outputAssertions.ts`.

Keep **`pnpm cypress run --spec e2e_test/features/cli/cli_install_and_run.feature`** (Nix wrapper per project rules) green when touching CLI E2E or shared harness.

---

## Unit test removal (Part 2 and any leftover groups)

1. Work **one test group / file** at a time (`describe` or whole file — smallest coherent unit).
2. Remove **code under test** and anything that **depends on it only** in the **same sub-phase** as test changes (see **Part 2 sub-phase rules**).
3. Collapse **empty or redundant** layers once behavior is gone.
4. **Tests:** Meaningful **user-centric** behavior → keep **`it` / `test`** with **`test.skip`** and **empty body** (title as reminder). **Low-level / trivial** → **delete** the test. See **observable behavior first** in `.cursor/rules/planning.mdc`.
5. **Stop** — developer review and commit.

### Part 2 sub-phase rules

- **Product + tests together** in one batch; no orphaned product code across commits.
- **`test.skip`** + empty body **only** for user-centric reminders (step 4); otherwise delete test and its exclusive product code.
- Prefer **safe deletion**; **`pnpm cli:test`** green after each batch.

---

## Part 2: Vitest cleanup (remaining work)

Part 1 is **complete:** non-install CLI scenarios are **`@ignore`**, E2E harness trimmed to install-only, interactive install path removed from active E2E, CLI/E2E rules updated.

**Per row:** steps above + one commit stop per group unless trivially empty.

| Phase | Target (confirm at execution time) |
|-------|-----------------------------------|
| 2.1 | ~~`cli/tests/interactive/`~~ **Done (TTY batch):** removed `interactiveTty*.test.ts` and `interactiveExitFarewell.test.ts`. **Still in folder:** `processInput.test.ts` (phase 2.6), `interactiveRendering.test.ts` (2.7), `interactiveTestHelpers.ts` / mocks (shared with phase 2.2). No product deletion this batch — recall + remaining Vitest still exercise `runInteractive` / TTY stack. |
| 2.2 | ~~`recall*.test.ts`, recall fixtures, `RecallInkConfirmPanel` / `recallYesNo.ts`, interactive `/recall` + TTY recall UI~~ **Done** — `recall.ts` retains `recallStatus` for `sdkHttpErrorClassification.test.ts`; `/recall` removed from help and `processInput`. |
| 2.3 | ~~`accessToken.test.ts`, `selectListInteraction.test.ts`, `listDisplay.test.ts`, `interactiveCommandInput.test.ts`, `userInputHistoryFile.test.ts`, `inputHistoryMask.test.ts`, `shell/pastMessagesModel.test.ts`~~ **Done** — tests removed; interactive shell still uses these modules (no orphan product). Stryker now uses `vitest.config.ts` + `related: false`; removed `vitest.stryker.config.ts`. |
| 2.4 | ~~`mainCommandLineInkTyping.test.ts`, `interactiveFetchWait.test.ts`, `ttyWriteSimulation.ts`~~ **Done** — tests/helpers removed; `interactiveFetchWait.ts`, `patchedTextInputKey.ts`, and fetch-abort wiring remain (used by TTY / `processInput`). |
| 2.5 | ~~`slashCompletion.test.ts`, `help.test.ts`~~ **Done** — removed `/help`, tab completion, slash picker, and `slashCompletion.ts`; `commands/help.ts` is `interactiveDocs` only; dropped `interactiveTestMocks.ts`; trimmed `interactiveCommandInput` (no tab/replace/slash-arrow helpers). |
| 2.6 | ~~`processInput.test.ts`~~ **Done** — `processInput` handles only `exit` / `/exit` and logs `Not supported` for any other non-empty line; removed param-command table and fetch-wait wiring from `interactive.ts`. `interactiveDocs` is `/exit` only; dropped `accessTokenCommandDocs`; adjusted token error copy. Trimmed `processInput` tests; renderer tests use `/exit` for param highlight; removed `makeTempConfigDir` / `withConfigDir` from interactive test helpers (were only used by removed cases). |
| 2.7 | `renderer.test.ts`, `interactive/interactiveRendering.test.ts` — keep if install shell needs them; else delete + unused renderer paths |
| 2.8 | `markdown.test.ts`, `sdkHttpErrorClassification.test.ts`, `index.test.ts` — keep if required for `version`/`update`/entry; else trim per skip/delete rules |
| 2.9 | **`gmail.test.ts`** — do not remove without changing the Gmail exception |
| 2.10 | **`version.test.ts`, `update.test.ts`** — align with install feature |
| 2.11 | `cli/vitest.config.ts`, Stryker configs — drop includes for deleted tests |

---

## Verification

- E2E: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_install_and_run.feature`
- CLI unit: `CURSOR_DEV=true nix develop -c pnpm cli:test`

---

## When finished

Delete or archive this file.
