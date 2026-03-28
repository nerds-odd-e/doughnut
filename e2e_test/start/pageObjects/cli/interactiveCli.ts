/**
 * Current installed interactive CLI (started by `installation().runInteractiveMode()`).
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
