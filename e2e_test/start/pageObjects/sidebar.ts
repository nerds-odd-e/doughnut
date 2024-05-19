export const sidebar = () => {
  cy.get("[role=sidebar]").should("exist")

  return {
    expectItems: (items) => {
      cy.get("[role=sidebar]").within(() => {
        cy.expectNoteCards(items)
      })
    },
  }
}
