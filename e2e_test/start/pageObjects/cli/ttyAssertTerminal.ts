/**
 * PTY lifecycle and I/O: `cy.task` names for interactive CLI (`runRepoCliInteractive`,
 * `cliInteractiveWriteLine`, `cliInteractiveAssert`, `cliInteractivePtyGetBuffer`, …).
 *
 * Transcript assertions are re-exported from `outputAssertions`. **Current guidance** uses
 * `cliInteractiveAssert`; **past assistant / answered / past user** still use buffer round-trips
 * until migrated. Do not use the Node `tty-assert/facade` `expect` API from Cypress (messages differ).
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
    enableGoogleOAuthSimulation() {
      return cy.task('cliInteractivePtyEnableGoogleOAuthSimulation')
    },
    answeredQuestions,
    pastCliAssistantMessages,
    pastUserMessages,
    currentGuidance,
    whenCurrentGuidanceContainsThen,
  }
}

export { ttyAssertTerminal }
