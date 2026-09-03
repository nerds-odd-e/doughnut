export const memoryTrackerRecallHistoryMethods = (onPage: () => void) => ({
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
