/**
 * Domain helpers on the interactive CLI PTY session (`ttyAssertTerminal` for `cy.task` I/O).
 *
 * **Assertions:** `pastCliAssistantMessages` and `answeredQuestions` search the **`strippedTranscript`**
 * surface. **`pastUserMessages.expectDisplayed`** runs **`fullBuffer`** gray-block checks plus
 * stripped-transcript blank-line layout in one Cypress retry loop (`outputAssertions`). **`currentGuidance`** uses xterm **viewport** replay plus Ink
 * heuristics — not the same surface. See `.cursor/rules/cli.mdc` terminology.
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
  return pty.submit(line)
}

function writeInteractiveRawToPty(data: string): Cypress.Chainable<null> {
  return pty.write(data)
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
      return whenCurrentGuidanceContainsThen(
        prompt,
        () => writeInteractiveLineToPty(answer),
        'cli-interactive-answer-to-prompt'
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
            ),
          'cli-interactive-recall-mcq-down-arrow-submit'
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
