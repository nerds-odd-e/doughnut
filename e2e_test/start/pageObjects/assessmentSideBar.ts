export const assessmentSidebar = () => {
  cy.get('aside').should('exist')

  return {
    expectItems: (items) => {
      cy.get('aside').within(() => {
        cy.expectNoteCards(items)
      })
    },
    expand: (noteTopic: string) => {
      cy.get('aside').within(() => {
        cy.findByText(noteTopic)
          .parent()
          .parent()
          .within(() => {
            cy.findByTitle('expand children').click()
          })
      })
    },
    siblingOrder: (higher: string, lower: string) => {
      cy.get('aside').within(() => {
        cy.contains(higher).parent().parent().nextAll().contains(lower)
      })
    },
  }
}
