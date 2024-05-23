export const sidebar = () => {
  cy.get("aside").should("exist")

  return {
    expectItems: (items) => {
      cy.get("aside").within(() => {
        cy.expectNoteCards(items)
      })
    },
    itemInViewport: (noteTopic: string) => {
      cy.get("aside").within(() => {
        cy.findByText(noteTopic).isInViewport()
      })
    },
    expand: (noteTopic: string) => {
      cy.get("aside").within(() => {
        cy.findByText(noteTopic)
          .parent()
          .within(() => {
            cy.findByTitle("expand children").click()
          })
      })
    },
  }
}
