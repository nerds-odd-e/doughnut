/**
 * Domain helpers on the interactive CLI PTY session (`ttyAssertTerminal` for `cy.task` I/O).
 *
 * **Assertions:** `currentGuidance`, **`whenCurrentGuidanceContainsThen`**, `pastCliAssistantMessages`,
 * `answeredQuestions`, and **`pastUserMessages.expectDisplayed`** all use **`cliAssert`**:
 * the plugin delegates to `tty-assert` managed-session `assert` (retry + replay in Node). Surfaces
 * differ — see `.cursor/rules/cli.mdc` terminology.
 */
import {
  answeredQuestions,
  currentGuidance,
  pastCliAssistantMessages,
  pastUserMessages,
  whenCurrentGuidanceContainsThen,
} from './outputAssertions'
import { ttyAssertTerminal } from './ttyAssertTerminal'

const pty = ttyAssertTerminal()

function writeInteractiveLineToPty(line: string): Cypress.Chainable<null> {
  return pty.submit(line) as Cypress.Chainable<null>
}

function writeInteractiveRawToPty(data: string): Cypress.Chainable<null> {
  return pty.write(data) as Cypress.Chainable<null>
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
    answerWhenPromptVisible(
      answer: string,
      prompt: string
    ): Cypress.Chainable<null> {
      return whenCurrentGuidanceContainsThen(prompt, () =>
        writeInteractiveLineToPty(answer)
      )
    },
    inputDownArrowSelectionForSlashCommand(
      command: string
    ): Cypress.Chainable<null> {
      return writeInteractiveLineToPty(command).then(() =>
        whenCurrentGuidanceContainsThen(
          'What is the meaning of sedition?',
          () =>
            writeInteractiveRawToPty('\u001b[B').then(() =>
              writeInteractiveRawToPty('\r')
            )
        )
      )
    },
    pastCliAssistantMessages,
    answeredQuestions,
    pastUserMessages,
    currentGuidance,
  }
}

export { interactiveCli }
