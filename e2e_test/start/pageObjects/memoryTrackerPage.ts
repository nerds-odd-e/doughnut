const assumeMemoryTrackerPage = () => {
  return {
    removeFromReview() {
      cy.findByRole('heading', { name: 'Memory Tracker' }).should('be.visible')
      cy.findByRole('button', {
        name: /remove this note from review/i,
      })
        .should('be.visible')
        .click()
      cy.findByRole('button', { name: 'OK' }).click()
      cy.findByText('This memory tracker has been removed from tracking.')
    },
  }
}

export { assumeMemoryTrackerPage }
