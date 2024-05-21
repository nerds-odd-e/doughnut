export const sidebar = () => {
  cy.get("aside").should("exist")

  return {
    expectItems: (items) => {
      cy.get("aside").within(() => {
        cy.expectNoteCards(items)
      })
    },
    expand: (noteTopic: string) => {
      cy.findByText(noteTopic).parent().within(() => {
        cy.findByRole("button", { name: "expand children" }).click()
      })
    },
  }
}
