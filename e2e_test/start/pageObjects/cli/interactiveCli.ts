/**
 * Interactive CLI PTY session: `@interactiveCLI` starts the repo bundle (`runRepoCliInteractive`);
 * install scenarios use `installation().runInteractiveMode()` (`runInstalledCliInteractive`).
 * Transcript assertions use `cliInteractivePtyGetBuffer` in the Cypress plugin.
 */
import { pastCliAssistantMessages, pastUserMessages } from './outputAssertions'

function writeInteractiveLineToPty(line: string) {
  cy.task('cliInteractiveWriteLine', { line })
}

function interactiveCli() {
  return {
    writeInteractiveLine(line: string) {
      writeInteractiveLineToPty(line)
    },
    enterSlashCommandInInteractiveCli(command: string) {
      writeInteractiveLineToPty(command)
    },
    pastCliAssistantMessages,
    pastUserMessages,
  }
}

export { interactiveCli }
