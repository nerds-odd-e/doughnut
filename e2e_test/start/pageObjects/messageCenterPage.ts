import { pageIsNotLoading } from '../pageBase'
import { mainMenu } from './mainMenu'

function withinConversationList(fn: () => void) {
  cy.findByText('Message Center').should('be.visible')
  pageIsNotLoading()
  cy.get('[data-testid="message-center-conversation-item"]').should(
    'have.length.at.least',
    1
  )
  cy.get('.message-center-container').within(fn)
}

export const assumeMessageCenterPage = () => {
  cy.findByText('Message Center').should('be.visible')

  return {
    expectConversation(subject: string, partner: string) {
      withinConversationList(() => {
        cy.findByText(subject).should('be.visible')
        cy.findByText(partner).should('be.visible')
      })
      return this
    },
    expectMessageDisplayAtUserSide(message: string) {
      cy.findByText(message).parents('.justify-end').should('be.visible')
      return this
    },
    expectMessageDisplayAtOtherSide(message: string) {
      cy.findByText(message)
        .parent()
        .should('be.visible')
        .and('not.have.class', 'justify-end')
      return this
    },
    conversation(conversationSubject: string) {
      withinConversationList(() => {
        cy.get(
          `[data-testid="message-center-conversation-item"][data-conversation-subject="${conversationSubject}"]`
        )
          .should('be.visible')
          .click()
      })
      pageIsNotLoading()
      return {
        expectMessage(message: string) {
          cy.findByText(message).should('be.visible')
          return this
        },
        reply(message: string) {
          cy.get('textarea').type(message)
          cy.findByRole('button', { name: 'Send message' }).click()
          cy.findByText(message).should('be.visible')
          return this
        },
      }
    },
  }
}

export const navigateToMessageCenter = () => {
  return mainMenu().myMessageCenter()
}
