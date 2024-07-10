import { assumeQuestionPage } from './QuizQuestionPage'

export function assumeChatAboutNotePage() {
  return {
    testMe() {
      cy.findByRole('button', { name: 'Test me' }).click()
      cy.pageIsNotLoading() // wait for the response
      return assumeQuestionPage()
    },
    sendMessage(msg: string) {
      cy.focused().type(msg)
      cy.get('#chat-button').click()
    },
    expectMessages(messages: Record<'role' | 'message', string>[]) {
      messages.forEach(({ role, message }) => {
        cy.findByText(message)
          .parents('.chat-answer-container')
          .should('have.class', role)
      })
    },
  }
}
