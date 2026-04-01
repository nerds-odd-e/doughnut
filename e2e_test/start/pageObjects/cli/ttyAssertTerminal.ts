/**
 * Thin Cypress wrappers for interactive CLI PTY `cy.task` handlers (Phase 1.5).
 * Centralizes task names; vocabulary aligns with `e2e_test/config/tty-assert-staging/facade`.
 *
 * Assertions delegate to `outputAssertions` — same retries, screenshots, and messages as
 * `interactiveCli()` (do not use Node facade `expect` from Cypress; error text differs).
 */

import {
  answeredQuestions,
  currentGuidance,
  pastCliAssistantMessages,
  pastUserMessages,
  whenCurrentGuidanceContainsThen,
} from './outputAssertions'

type RunRepoCliInteractiveArg = { env?: NodeJS.ProcessEnv }

type RunInstalledCliInteractiveArg = {
  doughnutPath: string
  env?: NodeJS.ProcessEnv
}

function ttyAssertTerminal() {
  return {
    startRepoInteractive(opts?: RunRepoCliInteractiveArg) {
      return cy.task('runRepoCliInteractive', opts ?? {})
    },
    startInstalledInteractive({
      doughnutPath,
      env,
    }: RunInstalledCliInteractiveArg) {
      return cy.task('runInstalledCliInteractive', { doughnutPath, env })
    },
    write(data: string) {
      return cy.task('cliInteractiveWriteRaw', { data })
    },
    submit(line: string) {
      return cy.task('cliInteractiveWriteLine', { line })
    },
    kill() {
      return cy.task('cliInteractivePtyDispose')
    },
    getRawBuffer() {
      return cy.task<string>('cliInteractivePtyGetBuffer')
    },
    enableGoogleOAuthSimulation() {
      return cy.task('cliInteractivePtyEnableGoogleOAuthSimulation')
    },
    answeredQuestions,
    pastCliAssistantMessages,
    pastUserMessages,
    currentGuidance,
    whenCurrentGuidanceContainsThen,
    /**
     * Opaque substring locator (facade-shaped). `expectVisibleInPastAssistantMessages` uses the
     * same check as `pastCliAssistantMessages().expectContains` — ANSI-stripped cumulative
     * transcript, not CSI-replayed viewport geometry.
     */
    getByText(value: string) {
      return {
        expectVisibleInPastAssistantMessages() {
          pastCliAssistantMessages().expectContains(value)
        },
      }
    },
  }
}

export { ttyAssertTerminal }
