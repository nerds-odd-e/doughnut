# End-to-end Testing

Donut uses [Cucumber](https://cucumber.io/docs/gherkin/),
[Cypress](https://docs.cypress.io/guides/getting-started/writing-your-first-test#Add-a-test-file),
the [Cypress Cucumber preprocessor](https://github.com/TheBrainFamily/cypress-cucumber-preprocessor),
and [Mountebank](https://github.com/mountebank-testing/mountebank) for
external-service mocks. Run Mountebank in debug mode with
`pnpm mb --loglevel debug`.

Typical workflow:

1. Run a feature with `pnpm cy:run --spec <feature-path>` (the wrapper owns its stack).
2. Develop or debug interactively with `pnpm cy:open`.
3. Run a feature headlessly with `pnpm cypress run --spec <feature-path>`.

Local `pnpm cy:run` batches compile current backend source once and retain Vite
for the frontend. After editing backend source, start a new batch. Interactive
`pnpm cy:open` sessions retain backend reload and frontend HMR. Built CI runs
use their existing frontend assets without Vite. Compilation failure stops the
invocation before Cypress can test stale backend classes, and the runner cleans
up its owned services. Existing build output needs no manual cleaning or
precompilation between ordinary batches.

WSL2 users must install `xvfb` outside Nix. Set
`NODE_OPTIONS="--max-old-space-size=4096"` before Cypress commands.

| Purpose | Command |
|---------|---------|
| Run E2E (owns its stack) | `pnpm cy:run --spec <feature>` (app at http://localhost:5173; [topology](./gcp/prod_env.md)) |
| Install E2E tooling | `pnpm --frozen-lockfile recursive install` |
| Start backend only | `pnpm backend:sut` |
| Start Mountebank only | `pnpm start:mb` |
| Open Cypress | `pnpm cy:open` |
| Run one feature | `pnpm cypress run --spec **/name.feature` |
| Run all E2E tests | `pnpm verify` |
| Regenerate TypeScript API | `pnpm generateTypeScript` |

## Retained Cucumber proof

Ordinary passing and failing test runs, including CI and test optimization,
leave retained proof untouched. Deliberately generate proof from the repository
root with the command declared in `.planning/open-dough.json`:

```bash
RECORD_CUCUMBER_PROOF=1 CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/note_creation.feature
```

Choose another supported feature or comma-separated feature selection with
`--spec` for a focused recording. `RECORD_CUCUMBER_PROOF=1` enables the shared
Cypress Messages recorder for both the wrapper and terminal Cypress runs;
tests, selection, reports and exit meaning stay the same.

Each completed scenario updates `e2e_test/proof/latest.json` under the checkout
lock. The snapshot keeps one success per identity in stable order, with tested
source, completion time, attempt count and total duration. Refresh reads every
nested proof file, selects each identity's latest observation (unsuccessful
timestamp ties win), keeps passes and consumes the input files. Retained
observations keep their original provenance. If no passes remain, the snapshot
is removed. Final executed failures clear earlier proof
for that identity; unexecuted scenarios keep theirs. Commit deliberately
generated proof and cleanup with the observed work or in an intentional proof
checkpoint. Recording never commits automatically. Published proof is the last
deliberately retained verification; CI results are independent observations.

## Structure

| Purpose | Location |
|---------|----------|
| Features | `e2e_test/features/*.feature` |
| Step definitions | `e2e_test/step_definitions/*.ts` |
| Custom DSL | `e2e_test/support/*.ts` |
| Cucumber hooks | `e2e_test/step_definitions/common/hook.ts` |
| Test fixtures | `e2e_test/fixtures/*.*` |
| Cypress config | `e2e_test/config/*.json` |
| Cypress plugins | `e2e_test/plugins/index.ts` |
