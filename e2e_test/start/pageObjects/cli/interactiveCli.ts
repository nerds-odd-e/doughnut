/**
 * Interactive CLI PTY session: `@interactiveCLI` starts the repo bundle (`runRepoCliInteractive`);
 * install scenarios use `installation().runInteractiveMode()` (`runInstalledCliInteractive`).
 * Transcript assertions use `cliInteractivePtyGetBuffer` in the Cypress plugin.
 */
import { pastCliAssistantMessages, pastUserMessages } from './outputAssertions'

function interactiveCli() {
  return {
    enterSlashCommandInInteractiveCli(command: string) {
      cy.task('cliInteractiveWriteLine', { line: command })
    },
    pastCliAssistantMessages,
    pastUserMessages,
  }
}

export { interactiveCli }
