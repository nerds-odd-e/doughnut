const recallLogs = () => cy.get('[data-testid="recall-log"]')

const recallHistoryItems = () => cy.get('[data-testid="recall-history-item"]')

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
  expectThinkingTimeUnderSeconds(seconds: number) {
    onPage()
    recallHistoryItems()
      .first()
      .find('[data-testid="recall-history-thinking-time"]')
      .should('be.visible')
      .invoke('attr', 'data-thinking-time-ms')
      .then((ms) => {
        expect(ms, 'thinking time milliseconds').to.exist
        expect(
          Number(ms),
          `expected thinking time ${ms}ms to be under ${seconds}s`
        ).to.be.lessThan(seconds * 1000)
      })
    return this
  },
  expectAwayTimeAndCount() {
    onPage()
    cy.get('[data-testid="recall-history-away-time"]')
      .should('be.visible')
      .and('contain.text', 'Away:')
      .and('contain.text', 'x)')
    return this
  },
  expectDetourTimeAndCount() {
    onPage()
    cy.get('[data-testid="recall-history-detour-time"]')
      .should('be.visible')
      .and('contain.text', 'Detour:')
      .and('contain.text', 'x)')
    cy.get('[data-testid="recall-history-away-time"]').should('not.exist')
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
