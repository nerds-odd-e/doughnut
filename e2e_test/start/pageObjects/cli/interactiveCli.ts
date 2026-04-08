/**
 * Domain helpers on the interactive CLI PTY session (`ttyAssertTerminal` for `cy.task` I/O).
 *
 * **Assertions:** `pastCliAssistantMessages` and `answeredQuestions` search the **`strippedTranscript`**
 * surface via `cliInteractivePtyGetBuffer` retries in `outputAssertions`. **`pastUserMessages.expectDisplayed`**
 * uses the same pattern (full buffer + stripped transcript per attempt). **`currentGuidance`** and
 * **`whenCurrentGuidanceContainsThen`** use **`cliInteractiveAssert`**: the plugin delegates to
 * `tty-assert` managed-session `assert` (viewport replay + Ink anchors), not browser-side buffer polling.
 * Surfaces differ — see `.cursor/rules/cli.mdc` terminology.
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
