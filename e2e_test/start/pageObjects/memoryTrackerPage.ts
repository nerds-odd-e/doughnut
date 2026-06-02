const SKIPPED_MEMORY_TRACKER_MESSAGE =
  'This memory tracker is currently skipped and will not appear in recall sessions.'

const expectMemoryTrackerPage = () => {
  cy.findByRole('heading', { name: 'Memory Tracker' }).should('be.visible')
}

const assumeMemoryTrackerPage = () => {
  return {
    removeFromRecall() {
      expectMemoryTrackerPage()
      cy.findByRole('button', {
        name: /remove this note from recall/i,
      })
        .should('be.visible')
        .click()
      cy.findByRole('button', { name: 'OK' }).click()
      cy.findByText(SKIPPED_MEMORY_TRACKER_MESSAGE)
      return assumeMemoryTrackerPage()
    },
    reviveMemoryTracker() {
      expectMemoryTrackerPage()
      cy.findByRole('button', {
        name: /revive this memory tracker/i,
      })
        .should('be.visible')
        .click()
      cy.findByText(SKIPPED_MEMORY_TRACKER_MESSAGE).should('not.exist')
      cy.findByRole('button', {
        name: /remove this note from recall/i,
      }).should('be.visible')
      return assumeMemoryTrackerPage()
    },
  }
}

export { assumeMemoryTrackerPage }
