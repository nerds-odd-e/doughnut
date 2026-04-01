/**
 * Interactive CLI PTY session: `@interactiveCLI` starts the repo bundle (`runRepoCliInteractive`);
 * install scenarios use `installation().runInteractiveMode()` (`runInstalledCliInteractive`).
 * PTY I/O task names live in `ttyAssertTerminal`; transcript assertions use `cliInteractivePtyGetBuffer` in the plugin.
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
