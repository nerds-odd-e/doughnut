import {
  EXCLUDED_ATTEMPT_NOTICE,
  expectExcludedAsAmbiguous,
} from './excludedAttemptNotice'
import { openPrivateRecallHelp } from './privateRecallHelp'

export const memoryTrackerRecallHistoryMethods = (onPage: () => void) => ({
  expectExcludedAttemptWithOutcome(reason: string, outcome: string) {
    onPage()
    cy.get('[data-testid="recall-history-item"]')
      .filter(`:has(${EXCLUDED_ATTEMPT_NOTICE})`)
      .as('excludedHistoryItem')
    expectExcludedAsAmbiguous(
      cy.get('@excludedHistoryItem').find(EXCLUDED_ATTEMPT_NOTICE),
      reason,
      'recall history item'
    )
    cy.get('@excludedHistoryItem')
      .find('[data-testid="recall-log-product-outcome"]')
      .should(($outcome) => {
        const actual = $outcome.text().trim()
        expect(
          actual,
          `Expected the excluded attempt's recall log to keep outcome ${outcome}, but found ${actual}`
        ).to.equal(outcome)
      })
    return this
  },
  getPrivateHelpForExcludedAttempt() {
    onPage()
    return openPrivateRecallHelp()
  },
  expectTutorFeedback(feedback: string) {
    onPage()
    cy.get('[data-testid="recall-log-tutor-feedback"]').should(($el) => {
      const actual = $el.text().trim()
      expect(
        actual,
        `Expected tutor feedback to be ${feedback}, but found ${actual}`
      ).to.equal(feedback)
    })
    return this
  },
})
