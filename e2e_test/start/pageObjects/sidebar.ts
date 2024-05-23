export const sidebar = () => {
  cy.get("aside").should("exist")

  return {
    expectItems: (items) => {
      cy.get("aside").within(() => {
        cy.expectNoteCards(items)
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
    moveItemToTopOfItsSiblings(noteTopic: string) {
      cy.get("aside").within(() => {
        cy.findByText(noteTopic)
      })
    },
    siblingOrder: (higher: string, lower: string) => {
      cy.get("aside").within(() => {
        cy.contains(higher).nextAll().contains(lower)
      })
    },
  }
}
