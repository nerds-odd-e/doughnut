import { systemSidebar } from './systemSidebar'

export const assumeMessageCenterPage = () => {
  cy.findByText('Message Center').should('be.visible')

  return {
    expectConversation(topic: string, partner: string) {
      cy.findByText(topic).should('be.visible')
      cy.findByText(partner).should('be.visible')
      return this
    },
    expectConvoWithPartner(partner: string) {
      cy.findByText(partner).should('be.visible')
      return this
    },
    clickToSeeExpectMessage(conversation: string, message: string) {
      cy.findByText(conversation).parent().should('be.visible').click()
      cy.findByText(message).should('be.visible')
      return this
    },
    replyInConversation(conversation: string, message: string) {
      cy.findByText(conversation).parent().should('be.visible').click()
      cy.get('textarea[name="Description"]').type(message)
      cy.get('input[type="submit"][value="Send"]').click()
      cy.findByText(message).should('be.visible')
      return this
    },
    expectMessageDisplayAtUserSide(message: string) {
      cy.findByText(message)
        .parent()
        .should('be.visible')
        .and('have.class', 'justify-content-end')
      return this
    },
    expectMessageDisplayAtOtherSide(message: string) {
      cy.findByText(message)
        .parent()
        .should('be.visible')
        .and('not.have.class', 'justify-content-end')
      return this
    },
    reloadCurrentPage() {
      cy.reload()
      return this
    },
  }
}

export const navigateToMessageCenter = () => {
  return systemSidebar().userOptions().myMessageCenter()
}
