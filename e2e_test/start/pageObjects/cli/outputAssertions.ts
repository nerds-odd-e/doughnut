import type { CliInteractiveAssertRequest } from '../../../config/cliInteractiveAssertRequest'
import {
  currentGuidanceContainsAssertRequest,
  currentGuidanceContainsBoldAssertRequest,
  waitForCurrentGuidancePromptAssertRequest,
} from './currentGuidanceCliAssertRequests'
import {
  answeredQuestionsContainsAssertRequest,
  pastCliAssistantMessagesContainsAssertRequest,
  pastUserMessageBlankLineAboveAssertRequest,
  pastUserMessageFullBufferGrayAssertRequest,
} from './transcriptCliAssertRequests'

/** Cypress task wall clock: must exceed managed-session `timeoutMs` used by CLI asserts. */
const CLI_INTERACTIVE_ASSERT_TASK_TIMEOUT_MS = 15_000

function runCliInteractiveAssertWithScreenshot(
  body: CliInteractiveAssertRequest,
  screenshotName: string
): Cypress.Chainable<void> {
  return cy.wrap(null, { log: false }).then((_subject: null) => {
    return new Cypress.Promise<void>((resolve, reject) => {
      const taskChain = cy.task('cliInteractiveAssert', body, {
        timeout: CLI_INTERACTIVE_ASSERT_TASK_TIMEOUT_MS,
      }) as Cypress.Chainable<null> & {
        then(
          onFulfilled: (value: null) => void,
          onRejected: (err: unknown) => void
        ): Cypress.Chainable<unknown>
      }
      taskChain.then(
        () => {
          resolve()
        },
        (err: unknown) => {
          cy.screenshot(screenshotName).then(() => {
            reject(err)
          })
        }
      )
    })
  })
}

/**
 * Waits until Current guidance contains `prompt`, then runs `onReady` (e.g. PTY write).
 * Assertion and retry run in the plugin via `cliInteractiveAssert` (managed PTY session).
 */
export function whenCurrentGuidanceContainsThen(
  prompt: string,
  onReady: () => Cypress.Chainable<null>,
  screenshotName: string
): Cypress.Chainable<null> {
  if (prompt === '') return onReady()
  return runCliInteractiveAssertWithScreenshot(
    waitForCurrentGuidancePromptAssertRequest(prompt),
    screenshotName
  ).then(() => onReady()) as Cypress.Chainable<null>
}

function pastCliAssistantMessages() {
  return {
    expectContains(
      expected: string,
      options?: { timeoutMs?: number }
    ): Cypress.Chainable<void> {
      if (expected === '')
        return cy.wrap(null, {
          log: false,
        }) as unknown as Cypress.Chainable<void>
      return runCliInteractiveAssertWithScreenshot(
        pastCliAssistantMessagesContainsAssertRequest(
          expected,
          options?.timeoutMs
        ),
        'cli-interactive-pty-past-assistant-assertion'
      )
    },
  }
}

function answeredQuestions() {
  return {
    expectContains(expected: string): Cypress.Chainable<void> {
      if (expected === '')
        return cy.wrap(null, {
          log: false,
        }) as unknown as Cypress.Chainable<void>
      return runCliInteractiveAssertWithScreenshot(
        answeredQuestionsContainsAssertRequest(expected),
        'cli-interactive-pty-answered-questions-assertion'
      )
    },
  }
}

function pastUserMessages() {
  return {
    /**
     * Full-buffer text + gray-block styling, then blank line above in the stripped transcript
     * (two `cliInteractiveAssert` tasks; retry lives in the managed session per request).
     */
    expectDisplayed(expected: string): Cypress.Chainable<void> {
      if (expected === '')
        return cy.wrap(null, {
          log: false,
        }) as unknown as Cypress.Chainable<void>
      return runCliInteractiveAssertWithScreenshot(
        pastUserMessageFullBufferGrayAssertRequest(expected),
        'cli-interactive-pty-past-user-displayed-full-buffer'
      ).then(() =>
        runCliInteractiveAssertWithScreenshot(
          pastUserMessageBlankLineAboveAssertRequest(expected),
          'cli-interactive-pty-past-user-displayed-blank-line'
        )
      )
    },
  }
}

function currentGuidance() {
  return {
    expectContains(expected: string): Cypress.Chainable<void> {
      if (expected === '')
        return cy.wrap(null, {
          log: false,
        }) as unknown as Cypress.Chainable<void>
      return runCliInteractiveAssertWithScreenshot(
        currentGuidanceContainsAssertRequest(expected),
        'cli-interactive-pty-current-guidance-assertion'
      )
    },
    expectContainsBold(text: string): Cypress.Chainable<void> {
      if (text === '')
        return cy.wrap(null, {
          log: false,
        }) as unknown as Cypress.Chainable<void>
      return runCliInteractiveAssertWithScreenshot(
        currentGuidanceContainsBoldAssertRequest(text),
        'cli-interactive-pty-current-guidance-bold-assertion'
      )
    },
  }
}

export {
  answeredQuestions,
  currentGuidance,
  pastCliAssistantMessages,
  pastUserMessages,
}
