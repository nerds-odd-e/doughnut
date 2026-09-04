# End-to-end Testing

Donut uses [Cucumber](https://cucumber.io/docs/gherkin/),
[Cypress](https://docs.cypress.io/guides/getting-started/writing-your-first-test#Add-a-test-file),
the [Cypress Cucumber preprocessor](https://github.com/TheBrainFamily/cypress-cucumber-preprocessor),
and [Mountebank](https://github.com/mountebank-testing/mountebank) for
external-service mocks. Run Mountebank in debug mode with
`pnpm mb --loglevel debug`.

Typical workflow:

1. Start the services with `pnpm sut`.
2. Develop or debug interactively with `pnpm cy:open`.
3. Run a feature headlessly with `pnpm cypress run --spec <feature-path>`.

WSL2 users must install `xvfb` outside Nix. Set
`NODE_OPTIONS="--max-old-space-size=4096"` before Cypress commands.

| Purpose | Command |
|---------|---------|
| Start the full environment | `pnpm sut` (app at http://localhost:5173; [topology](./gcp/prod_env.md)) |
| Install E2E tooling | `pnpm --frozen-lockfile recursive install` |
| Start backend only | `pnpm backend:sut` |
| Start Mountebank only | `pnpm start:mb` |
| Open Cypress | `pnpm cy:open` |
| Run one feature | `pnpm cypress run --spec **/name.feature` |
| Run all E2E tests | `pnpm verify` |
| Regenerate TypeScript API | `pnpm generateTypeScript` |

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
