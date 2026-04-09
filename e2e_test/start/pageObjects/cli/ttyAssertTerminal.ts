/**
 * PTY lifecycle and I/O: `cy.task` names for interactive CLI (`runRepoCliInteractive`,
 * `cliInteractiveWriteLine`, `cliAssert`, `cliInteractivePtyDispose`, …).
 *
 * Assertions use **`assert`** / shared **`cliAssertTask`** → `cliAssert` with JSON-serializable
 * payloads; the plugin delegates to `tty-assert` managed session. Request builders live in
 * `outputAssertions.ts`.
 */

import type { ManagedTtyAssertTaskPayload } from '../../../config/cliE2ePluginTasks'
import { cliAssertTask } from './cliAssertTask'
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
    assert(body: ManagedTtyAssertTaskPayload) {
      return cliAssertTask(body)
    },
    answeredQuestions,
    pastCliAssistantMessages,
    pastUserMessages,
    currentGuidance,
    whenCurrentGuidanceContainsThen,
  }
}

export { ttyAssertTerminal }
