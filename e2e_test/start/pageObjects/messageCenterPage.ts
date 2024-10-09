import { systemSidebar } from './systemSidebar'

export const assumeMessageCenterPage = () => {
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
      cy.findByText(question).should('be.visible').click()
      cy.findByText(message).should('be.visible')
    },
    expectButton(message: string, partner: string) {
      cy.findByTestId(message).should('be.visible')
      cy.findByText(partner).should('be.visible')
      return this
    },
  }
}

export const navigateToMessageCenter = () => {
  return systemSidebar().userOptions().myMessageCenter()
}
