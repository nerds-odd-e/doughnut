import { systemSidebar } from './systemSidebar'

export const assumeMessageCenterPage = () => {
  cy.findByText('Message Center').should('be.visible')

  return {
    expectMessage(message: string, partner: string) {
      cy.findByText(message).should('be.visible')
      cy.findByText(partner).should('be.visible')
      return this
    },
    expectSingleMessage(message: string) {
      cy.findByText(message).should('be.visible')
      return this
    },
    clickToSeeExpectMessage(question: string, message: string) {
      cy.findByText(question).parent().should('be.visible').click()
      cy.findByText(message).should('be.visible')
      return this
    },
    clickToSeeExpectForm(question: string) {
      cy.findByText(question).parent().should('be.visible').click()
      cy.get('textarea[name="Description"]').should('be.visible')
      cy.get('input[type="submit"][value="Send"]').should('be.visible')
      return this
    },
    expectButton(message: string, partner: string) {
      cy.findByTestId(message).should('be.visible')
      cy.findByText(partner).should('be.visible')
      return this
    },
    typeAndSendMessage(message: string) {
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
  }
}

export const navigateToMessageCenter = () => {
  return systemSidebar().userOptions().myMessageCenter()
}
