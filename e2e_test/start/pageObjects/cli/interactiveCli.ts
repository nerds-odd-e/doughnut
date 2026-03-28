/**
 * Current installed interactive CLI: reads `@cliInteractivePtyOutput` (started by
 * `installation().runInteractiveMode()`). PTY handle lives in `interactiveCliPtySession`
 */
import { pastCliAssistantMessages, pastUserMessages } from './outputAssertions'

function interactiveCli() {
  return {
    enterSlashCommandInInteractiveCli(command: string) {
      cy.get<string>('@cliInteractivePtyOutput')
      cy.task<string>('cliInteractiveWriteLine', { line: command }).as(
        'cliInteractivePtyOutput'
      )
    },
    pastCliAssistantMessages,
    pastUserMessages,
  }
}

export { interactiveCli }
