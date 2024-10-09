import { systemSidebar } from './systemSidebar'

export const assumeMessageCenterPage = () => {
  return {
    expectMessage(message: string, partner: string) {
      cy.findByText(message).should('be.visible')
      cy.findByText(partner).should('be.visible')
      return this
    },
    expectDefaultMessage(message: string) {
      cy.findByText(message).should('be.visible')
      return this
    },
  }
}

export const navigateToMessageCenter = () => {
  return systemSidebar().userOptions().myMessageCenter()
}
