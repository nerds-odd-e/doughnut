# Plan: Refactor CLI Step Definitions to Page Object Pattern

## Goal

Refactor `e2e_test/step_definitions/cli.ts` to use the Page Object pattern with fluent interface, aligned with other step definitions (e.g. `start.xxx`). CLI page objects will use the `cli.xxx` prefix and map directly to domain concepts and UI state. Eventually, all `cy.get` and `cy.task` calls will live inside page objects with a fluent interface; step definitions only orchestrate.

## Current State

- **cli.ts** (~136 lines): Step definitions delegate to `cli.*` page objects. Output assertions, execution, recall, and remove-token steps all use page objects. Still contains: Given for backend install check, Gmail mocks (Given), Gmail When steps via `cli.gmail()`.
- **cli/page objects** (`e2e_test/start/pageObjects/cli/`): `outputAssertions.ts`, `execution.ts`, `recallSession.ts`, `removeToken.ts`, `backend.ts`, `index.ts` – Phases 1–4 done.
- **cliSectionParser.ts**: Domain-aware parser for output sections (history-output, history-input, current-guidance); used by page objects
- **Domain concepts** (from cli.mdc):
  - **Non-interactive output**: Entire stdout when running with `-c`, piped, or installed binary
  - **History output**: Interactive only. Past command results.
  - **History input**: Interactive only. Past user input lines.
  - **Current guidance**: Interactive only. Prompts, hints, options for current input.
  - **Input box**: The bordered area where the user types.

## Target Architecture

### Page object namespace: `cli`

Entry point: `e2e_test/start/pageObjects/cli/index.ts` exporting `cli` object.

### Domain-mapped page objects

| Page Object | Domain Concept | Methods (fluent where applicable) |
|-------------|-----------------|------------------------------------|
| `cli.nonInteractiveOutput()` | Non-interactive output | `expectContains(expected)`, `expectNotContains(expected)` |
| `cli.historyOutput()` | History output | `expectContains(expected)`, `expectNotContains(expected)` |
| `cli.historyInput()` | History input | `expectContains(expected)` |
| `cli.currentGuidance()` | Current guidance | `expectContains(expected)`, `expectStyled(expected)` |

All output-based page objects read from `cy.get<string>('@doughnutOutput')` internally and return `this` for fluent chaining where it makes sense.

### Execution page objects (actions)

| Page Object | Responsibility | Methods |
|-------------|----------------|---------|
| `cli.installation()` | Install and run installed binary | `installFromLocalhost()`, `runInstalled()`, `runVersion()`, `runUpdate(baseUrl)` |
| `cli.nonInteractive()` | Run non-interactive commands | `runWithInput(input)`, `runWithCommand(cmd)`, `runWithConfig(args)` |
| `cli.interactive()` | Send input to interactive CLI | `input(text)`, `pressEsc()`, `answerToPrompt(answer, expectedPrompt)`, `inputDownArrowSelection(command)` |
| `cli.accessToken()` | Access token commands | `runWithSavedToken(command)`, `runWithToken(command, token)`, `runWithLabel(command, label)` |
| `cli.gmail()` | Gmail commands | `addWithSimulatedOAuth()`, `lastEmailWithPreconfiguredAccount()` |

### Recall-session assertions (domain-specific)

| Page Object | Responsibility |
|-------------|----------------|
| `cli.recallSession()` | Recall session state | `expectStopped()`, `expectStoppedDuringReview()` |

### Setup and verification

| Page Object | Responsibility |
|-------------|----------------|
| `cli.backend()` | Backend serving CLI | `expectInstallScriptServed()` (Phase 4) |
| `cli.removeToken()` | Access-token removal success | `expectSuccess(removalType, label)` – local vs complete |

## File Structure

**Current (grouped)**:
- `outputAssertions.ts` – nonInteractiveOutput, historyOutput, historyInput, currentGuidance; exports `withOutput` for other output readers
- `execution.ts` – installation, nonInteractive, interactive, accessToken, gmail
- `backend.ts` – backend serving CLI (expectInstallScriptServed, serveVersion)
- `recallSession.ts` – recall session state (expectStopped, expectStoppedDuringReview)
- `removeToken.ts` – access-token removal success
- `index.ts` – exports `cli`, grouped by domain: output sections → recall → removeToken → execution

Prefer grouped files; split only if a file exceeds ~150 lines.

## Step definition refactor

### Pattern

```typescript
// Before
Then('I should see {string} in the non-interactive output', (expected: string) => {
  cy.get<string>('@doughnutOutput').then((output) =>
    assertOutputIncludes(output, expected, NON_INTERACTIVE_OUTPUT)
  )
})

// After
Then('I should see {string} in the non-interactive output', (expected: string) => {
  cli.nonInteractiveOutput().expectContains(expected)
})
```

### Phase 1: Output assertions ✅

1. ~~Create~~ `cli/outputAssertions.ts` (combined) – `nonInteractiveOutput`, `historyOutput`, `historyInput`, `currentGuidance`.
2. Each exposes `expectContains` / `expectNotContains` / `expectStyled`; uses cliSectionParser; domain-specific error messages.
3. ~~Refactor~~ Then steps delegate to `cli.nonInteractiveOutput()`, `cli.historyOutput()`, etc.
4. ~~Remove~~ `assertOutputIncludes`, `assertOutputNotIncludes`, `buildCurrentGuidanceFailureMessage` from cli.ts.

### Phase 2: Execution (When steps) ✅

1. ~~Create~~ `cli/execution.ts` (combined) – `installation`, `nonInteractive`, `interactive`, `accessToken`, `gmail`, `backend`.
2. ~~Move~~ `runDoughnutWithConfig`, `runDoughnutWithConfigCommand`, `cliEnvWithConfigDir` into page objects.
3. ~~Refactor~~ When steps to use `cli.installation().installFromLocalhost()`, `cli.nonInteractive().runWithInput(...)`, etc.
4. ~~Interactive steps~~ `cli.interactive().input(...)`, `cli.interactive().pressEsc()`, etc.

### Phase 3: Recall and removal assertions ✅

1. ~~Create~~ `cli/recallSession.ts` for `the recall session was stopped`, `I stopped the recall during review` – `expectStopped()` (MCQ), `expectStoppedDuringReview()` (y/n review).
2. ~~Create~~ `cli/removeToken.ts` for `I should see the {word} remove success message for {string}` (`cli.removeToken().expectSuccess(removalType, label)`).
3. ~~Refactor~~ those Then steps.
4. ~~Cleanup~~: Deduplicate `withOutput` (export from outputAssertions, import in recallSession). Group `cli` index by domain. Add domain JSDoc (MCQ vs review, local vs complete removal).

### Phase 4: Setup and Gmail mocks ✅

1. ~~Create~~ `cli/backend.ts` for `the backend is serving the CLI and install script` – `expectInstallScriptServed()`, `serveVersion()`.
2. Gmail Given steps (Google API mock) stay in cli.ts; they use `mock_services` which is a shared dependency.
3. ~~Gmail When steps~~: `cli.gmail().addWithSimulatedOAuth()`, `cli.gmail().lastEmailWithPreconfiguredAccount()` (already in place).

### Phase 5: Slim common.ts and replace overkill tasks ✅

1. ~~Audit CLI-related tasks in common.ts; identify domain logic vs. plumbing.~~
2. ~~Move config setup (gmail.json, temp dirs) to page objects or step setup.~~
3. ~~Replace `runCliDirectWithGmailAdd`, `runCliDirectWithLastEmail` with `runCliDirectWithInput` or `runCliDirectWithArgs` + setup orchestrated by `cli.gmail()` page object.~~
4. ~~Keep in common.ts only: generic spawn helpers, `getCliRunConfig`, env wiring. Delete bespoke task implementations once replaced.~~

Done: Added `createCliConfigDirWithGmail(gmailConfig)` task. Extended `runCliDirectWithInput` with optional `simulateOAuthCallback`. `cli.gmail()` now orchestrates config creation + `runCliDirectWithInput`. Removed `runCliDirectWithGmailAdd` and `runCliDirectWithLastEmail`.

### Phase 6: Full encapsulation—cy.get and cy.task in page objects ✅

1. ~~Ensure every CLI step delegates to a page object method.~~
2. ~~Page objects own: `cy.get('@doughnutOutput')`, `cy.get('@doughnutPath')`, `cy.get('@cliConfigDir')`, and all `cy.task(...)` invocations.~~
3. ~~Step definitions become one-liners: `When('...', () => cli.gmail().lastEmailWithPreconfiguredAccount())`.~~

Done: Added `cli.backend().bundleAndCopy()`, `cli.setup().createConfigDir()`, `cli.interactive().startSession()`, `cli.interactive().stopSession()`. Hooks in hook.ts now delegate to cli page objects; no `cy.get` or `cy.task` remain in CLI step definitions or hooks for CLI concerns.

### Phase 7: Integration and verification

1. ~~`cli/index.ts` exists and exports `cli`~~ (done).
2. ~~`cli` imported in `cli.ts` step definitions; CLI remains its own namespace~~.
3. Run all CLI feature files to verify: `cli_access_token.feature`, `cli_recall.feature`, `cli_install_and_run.feature`, `cli_gmail.feature`.
4. Delete unused helpers from cli.ts after each phase.

## Cursor rules to update

### e2e_test.mdc

- Under "Project Structure": Add that `e2e_test/start/pageObjects/cli/` contains CLI page objects (prefix `cli.xxx`).
- Under "Page Object Pattern": Add example showing `cli.nonInteractiveOutput().expectContains(expected)` for CLI assertions.
- Optional: Add a short subsection "CLI E2E and page objects" stating that CLI steps use `cli.xxx` page objects, not `start.xxx`.

### cli.mdc

- Under "CLI E2E tests": Replace or augment the E2E assertion steps description to mention that step definitions delegate to `cli.xxx` page objects.
- Add: "CLI page objects live in `e2e_test/start/pageObjects/cli/` and map to domain concepts: non-interactive output, history output, history input, current guidance. Use `cli.nonInteractiveOutput()`, `cli.currentGuidance()`, etc."

## Verification

- All CLI feature files pass: `pnpm cypress run --spec e2e_test/features/cli/*.feature`
- No regression in step definition step text (Gherkin unchanged).
- `pnpm format:all` and `pnpm lint:all` pass.

## Out of scope

- Changing Gherkin scenarios or step text.
- Modifying cliSectionParser API (page objects will consume it as-is).
- Adding cli to `start` object; CLI remains a separate import in step definitions.
