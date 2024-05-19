export const sidebar = () => {
  cy.get("[role=sidebar]").should("exist")
}
