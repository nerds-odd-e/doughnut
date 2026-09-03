const recallLogs = () => cy.get('[data-testid="recall-log"]')

export const memoryTrackerRecallHistoryMethods = (onPage: () => void) => ({
  expectGoodRecallLogWithoutAnswer() {
    onPage()
    recallLogs().should('have.length', 1)
    cy.get('[data-testid="recall-log-product-outcome"]').should(
      'have.text',
      'GOOD'
    )
    recallLogs().should('contain.text', 'Recorded:')
    recallLogs().should('contain.text', 'Elapsed hours:')
    cy.get('[data-testid="recall-log-answer-id"]').should('not.exist')
    return this
  },
  expectAgainRecallLog() {
    onPage()
    recallLogs().should('have.length', 2)
    recallLogs().contains('[data-testid="recall-log-product-outcome"]', 'AGAIN')
    return this
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
