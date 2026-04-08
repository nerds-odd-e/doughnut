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

function cliInteractiveAssert(
  body: CliInteractiveAssertRequest
): Cypress.Chainable<null> {
  return cy.task('cliInteractiveAssert', body) as Cypress.Chainable<null>
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
      return cliInteractiveAssert(
        pastCliAssistantMessagesContainsAssertRequest(
          expected,
          options?.timeoutMs
        )
      ) as unknown as Cypress.Chainable<void>
    },
  }
}

function answeredQuestions() {
  return {
    expectContains(expected: string): Cypress.Chainable<void> {
      return cliInteractiveAssert(
        answeredQuestionsContainsAssertRequest(expected)
      ) as unknown as Cypress.Chainable<void>
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
      return cliInteractiveAssert(
        pastUserMessageFullBufferGrayAssertRequest(expected)
      ).then(() =>
        cliInteractiveAssert(
          pastUserMessageBlankLineAboveAssertRequest(expected)
        )
      ) as unknown as Cypress.Chainable<void>
    },
  }
}

function currentGuidance() {
  return {
    expectContains(expected: string): Cypress.Chainable<void> {
      return cliInteractiveAssert(
        currentGuidanceContainsAssertRequest(expected)
      ) as unknown as Cypress.Chainable<void>
    },
    expectContainsBold(text: string): Cypress.Chainable<void> {
      return cliInteractiveAssert(
        currentGuidanceContainsBoldAssertRequest(text)
      ) as unknown as Cypress.Chainable<void>
    },
  }
}

export {
  answeredQuestions,
  currentGuidance,
  pastCliAssistantMessages,
  pastUserMessages,
}
