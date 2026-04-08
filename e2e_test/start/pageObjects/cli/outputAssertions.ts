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

function cliInteractiveAssert(
  body: CliInteractiveAssertRequest
): Cypress.Chainable<null> {
  return cy.task('cliInteractiveAssert', body, {
    timeout: CLI_INTERACTIVE_ASSERT_TASK_TIMEOUT_MS,
  }) as Cypress.Chainable<null>
}

function unlessEmpty(
  s: string,
  run: () => Cypress.Chainable<null>
): Cypress.Chainable<void> {
  if (s === '')
    return cy.wrap(null, { log: false }) as unknown as Cypress.Chainable<void>
  return run() as unknown as Cypress.Chainable<void>
}

/**
 * Waits until Current guidance contains `prompt`, then runs `onReady` (e.g. PTY write).
 * Assertion and retry run in the plugin via `cliInteractiveAssert` (managed PTY session).
 */
export function whenCurrentGuidanceContainsThen(
  prompt: string,
  onReady: () => Cypress.Chainable<null>
): Cypress.Chainable<null> {
  if (prompt === '') return onReady()
  return cliInteractiveAssert(
    waitForCurrentGuidancePromptAssertRequest(prompt)
  ).then(() => onReady()) as Cypress.Chainable<null>
}

function pastCliAssistantMessages() {
  return {
    expectContains(
      expected: string,
      options?: { timeoutMs?: number }
    ): Cypress.Chainable<void> {
      return unlessEmpty(expected, () =>
        cliInteractiveAssert(
          pastCliAssistantMessagesContainsAssertRequest(
            expected,
            options?.timeoutMs
          )
        )
      )
    },
  }
}

function answeredQuestions() {
  return {
    expectContains(expected: string): Cypress.Chainable<void> {
      return unlessEmpty(expected, () =>
        cliInteractiveAssert(answeredQuestionsContainsAssertRequest(expected))
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
      return unlessEmpty(expected, () =>
        cliInteractiveAssert(
          pastUserMessageFullBufferGrayAssertRequest(expected)
        ).then(() =>
          cliInteractiveAssert(
            pastUserMessageBlankLineAboveAssertRequest(expected)
          )
        )
      )
    },
  }
}

function currentGuidance() {
  return {
    expectContains(expected: string): Cypress.Chainable<void> {
      return unlessEmpty(expected, () =>
        cliInteractiveAssert(currentGuidanceContainsAssertRequest(expected))
      )
    },
    expectContainsBold(text: string): Cypress.Chainable<void> {
      return unlessEmpty(text, () =>
        cliInteractiveAssert(currentGuidanceContainsBoldAssertRequest(text))
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
