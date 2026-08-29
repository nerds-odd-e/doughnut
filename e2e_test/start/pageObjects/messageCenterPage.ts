import { waitUntilAppIsNotBusy } from '../pageBase'
import router from 'start/router'

function conversationPane() {
  return {
    expectMessage(message: string) {
      cy.findByText(message).should(($el) => {
        const actual = $el.text().trim()
        expect(
          actual,
          `Expected conversation message "${message}", but found "${actual}"`
        ).to.equal(message)
      })
      return this
    },
  }
}

function withinConversationList(fn: () => void) {
  cy.findByText('Message Center').should('be.visible')
  waitUntilAppIsNotBusy()
  cy.get('[data-testid="message-center-conversation-item"]').should(
    'have.length.at.least',
    1
  )
  cy.get('.message-center-container').within(fn)
}

export const assumeMessageCenterPage = () => {
  cy.findByText('Message Center').should('be.visible')

  return {
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
      waitUntilAppIsNotBusy()
      return conversationPane()
    },
  }
}

const interceptConversationList = () => {
  cy.intercept('GET', '**/api/conversation/all').as('conversationList')
}

const waitForConversationList = (options?: { expectedSubject?: string }) => {
  cy.wait('@conversationList').should(({ response }) => {
    expect(response?.statusCode, 'load message center conversations').to.equal(
      200
    )
    if (options?.expectedSubject) {
      const conversations = response?.body as
        | Array<{ subject: string }>
        | undefined
      expect(
        conversations?.some((item) => item.subject === options.expectedSubject),
        `conversation list should include subject "${options.expectedSubject}"`
      ).to.be.true
    }
  })
  waitUntilAppIsNotBusy()
}

export const navigateToMessageCenter = (options?: {
  expectedSubject?: string
}) => {
  interceptConversationList()
  router().push('messageCenter')
  waitForConversationList(options)
  return assumeMessageCenterPage()
}
