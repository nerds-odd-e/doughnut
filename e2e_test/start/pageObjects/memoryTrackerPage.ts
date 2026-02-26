const assumeMemoryTrackerPage = () => {
  return {
    removeFromRecall() {
      cy.findByRole('heading', { name: 'Memory Tracker' }).should('be.visible')
      cy.findByRole('button', {
        name: /remove this note from recall/i,
      })
        .should('be.visible')
        .click()
      cy.findByRole('button', { name: 'OK' }).click()
      cy.findByText(
        'This memory tracker is currently skipped and will not appear in recall sessions.'
      )
    },
  }
}

export { assumeMemoryTrackerPage }
