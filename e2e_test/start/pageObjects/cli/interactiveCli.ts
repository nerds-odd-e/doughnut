/**
 * Interactive CLI PTY session: `@interactiveCLI` starts the repo bundle (`runRepoCliInteractive`);
 * install scenarios use `installation().runInteractiveMode()` (`runInstalledCliInteractive`).
 * Transcript assertions use `cliInteractivePtyGetBuffer` in the Cypress plugin.
 */
import {
  currentGuidance,
  pastCliAssistantMessages,
  pastUserMessages,
} from './outputAssertions'

function writeInteractiveLineToPty(line: string): Cypress.Chainable<null> {
  return cy.task('cliInteractiveWriteLine', { line })
}

function interactiveCli() {
  return {
    writeInteractiveLine(line: string): Cypress.Chainable<null> {
      return writeInteractiveLineToPty(line)
    },
    enterSlashCommandInInteractiveCli(
      command: string
    ): Cypress.Chainable<null> {
      return writeInteractiveLineToPty(command)
    },
    pastCliAssistantMessages,
    pastUserMessages,
    currentGuidance,
  }
}

export { interactiveCli }
