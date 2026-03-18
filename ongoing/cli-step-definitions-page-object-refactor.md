# Plan: Refactor CLI Step Definitions to Page Object Pattern

## Goal

Refactor `e2e_test/step_definitions/cli.ts` to use the Page Object pattern with fluent interface, aligned with other step definitions (e.g. `start.xxx`). CLI page objects will use the `cli.xxx` prefix and map directly to domain concepts and UI state.

## Current State

- **cli.ts** (~360 lines): Step definitions with inline logic, helpers like `assertOutputIncludes`, `runDoughnutWithConfig`, `buildCurrentGuidanceFailureMessage`
- **cliSectionParser.ts**: Domain-aware parser for output sections (history-output, history-input, current-guidance); used by step definitions
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
| `cli.backend()` | Backend serving CLI | `expectInstallScriptServed()` |
| `cli.removeToken()` | Token removal expectations | `expectLocalRemoveSuccess(label)`, `expectCompleteRemoveSuccess(label)` |

## File Structure

```
e2e_test/start/pageObjects/cli/
  index.ts           # Exports `cli` object aggregating all sub-page objects
  nonInteractiveOutput.ts
  historyOutput.ts
  historyInput.ts
  currentGuidance.ts
  installation.ts
  nonInteractiveExecution.ts
  interactiveExecution.ts
  accessTokenExecution.ts
  recallSession.ts
  gmailExecution.ts
```

**Alternative**: Fewer files with grouped concerns:
- `outputAssertions.ts` – nonInteractiveOutput, historyOutput, historyInput, currentGuidance (all read from @doughnutOutput)
- `execution.ts` – installation, nonInteractive, interactive, accessToken, gmail
- `recallSession.ts` – recall-specific assertions
- `index.ts` – exports `cli`

Prefer the grouped approach for cohesion; split only if a file exceeds ~150 lines.

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

### Phase 1: Output assertions

1. Create `cli/nonInteractiveOutput.ts`, `historyOutput.ts`, `historyInput.ts`, `currentGuidance.ts` (or combined `outputAssertions.ts`).
2. Each exposes methods that:
   - Resolve `cy.get<string>('@doughnutOutput')`
   - Use cliSectionParser (or encapsulate its logic)
   - Assert with domain-specific error messages (keep `buildCurrentGuidanceFailureMessage` in currentGuidance)
3. Refactor Then steps for output assertions to delegate to these page objects.
4. Remove `assertOutputIncludes`, `assertOutputNotIncludes`, `buildCurrentGuidanceFailureMessage` from cli.ts (move into page objects).

### Phase 2: Execution (When steps)

1. Create `cli/installation.ts`, `nonInteractiveExecution.ts`, `interactiveExecution.ts`, `accessTokenExecution.ts`, `gmailExecution.ts` (or combined `execution.ts`).
2. Move `runDoughnutWithConfig`, `runDoughnutWithConfigCommand`, env helpers into page objects.
3. Refactor When steps to use `cli.installation().installFromLocalhost()`, `cli.nonInteractive().runWithInput(...)`, etc.
4. Interactive steps: `cli.interactive().input(...)`, `cli.interactive().pressEsc()`, etc.

### Phase 3: Recall and removal assertions

1. Create `cli/recallSession.ts` for `the recall session was stopped`, `I stopped the recall during review`.
2. Create or extend page object for `I should see the {word} remove success message for {string}` (e.g. `cli.removeToken().expectSuccess(removalType, label)`).
3. Refactor those Then steps.

### Phase 4: Setup and Gmail mocks

1. Create `cli/backend.ts` for `the backend is serving the CLI and install script`.
2. Gmail Given steps (Google API mock) stay in cli.ts or move to a `cli/gmailMocks.ts` if desired; they use `mock_services` which is a shared dependency.
3. Gmail When steps: `cli.gmail().addWithSimulatedOAuth()`, `cli.gmail().lastEmailWithPreconfiguredAccount()`.

### Phase 5: Integration and export

1. Create `cli/index.ts` exporting `cli` object.
2. Import `cli` in `cli.ts` step definitions (do not add to `start`; CLI has its own namespace).
3. Run all CLI feature files to verify: `cli_access_token.feature`, `cli_recall.feature`, `cli_install_and_run.feature`, `cli_gmail.feature`.
4. Delete unused helpers from cli.ts.

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
