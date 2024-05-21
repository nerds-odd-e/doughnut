export const sidebar = () => {
  cy.get("aside").should("exist")

  return {
    expectItems: (items) => {
      cy.get("aside").within(() => {
        cy.expectNoteCards(items)
      })
    },
  }
}
