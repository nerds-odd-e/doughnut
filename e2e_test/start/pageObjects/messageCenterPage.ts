import { pageIsNotLoading } from '../pageBase'
import router from 'start/router'

function conversationPane() {
  return {
    expectMessage(message: string) {
      cy.findByText(message).should('be.visible')
      return this
    },
    reply(message: string) {
      cy.get('textarea')
        .should('be.visible')
        .and('be.enabled')
        .clear()
        .type(message)
      cy.findByRole('button', { name: 'Send message' }).click()
      cy.findByText(message).should('be.visible')
      pageIsNotLoading()
      return this
    },
  }
}

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
    openConversation(subject: string, partner: string) {
      withinConversationList(() => {
        cy.findByText(subject).should('be.visible')
        cy.findByText(partner).should('be.visible')
        cy.get(
          `[data-testid="message-center-conversation-item"][data-conversation-subject="${subject}"]`
        )
          .should('be.visible')
          .click()
      })
      pageIsNotLoading()
      return conversationPane()
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
      return conversationPane()
    },
    replyToConversation(
      conversationSubject: string,
      messages: readonly string[]
    ) {
      this.conversation(conversationSubject)
      cy.wrap(messages).each((message: string) => {
        cy.get('textarea')
          .should('be.visible')
          .and('be.enabled')
          .clear()
          .type(message)
        cy.findByRole('button', { name: 'Send message' }).click()
        cy.findByText(message).should('be.visible')
        pageIsNotLoading()
      })
      return this
    },
  }
}

export const navigateToMessageCenter = () => {
  router().toMessageCenter()
  pageIsNotLoading()
  return assumeMessageCenterPage()
}
