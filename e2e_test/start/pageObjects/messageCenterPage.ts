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
    clickToSeeExpectMessage(conversation: string, message: string) {
      return this.conversation(conversation).expectMessage(message)
    },
    conversation(conversationTopic: string) {
      cy.findByText(conversationTopic).parent().should('be.visible').click()
      return {
        expectMessage(message: string) {
          cy.findByText(message).should('be.visible')
          return this
        },
        reply(message: string) {
          cy.get('textarea[name="Description"]').type(message)
          cy.get('input[type="submit"][value="Send"]').click()
          cy.findByText(message).should('be.visible')
          return this
        },
      }
    },
  }
}

export const navigateToMessageCenter = () => {
  return systemSidebar().userOptions().myMessageCenter()
}
