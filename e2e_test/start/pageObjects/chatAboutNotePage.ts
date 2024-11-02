import { assumeQuestionPage } from './QuizQuestionPage'

export function assumeChatAboutNotePage() {
  return {
    testMe() {
      cy.findByRole('button', { name: 'Test me' }).click()
      cy.pageIsNotLoading() // wait for the response
      return assumeQuestionPage()
    },
    replyToConversation(msg: string) {
      cy.focused().type(msg)
      cy.get('#chat-button').click()
    },
    replyToConversationNew(msg: string) {
      cy.focused().type(msg)
      cy.findByRole('button', {
        name: 'Send message and invite AI to reply',
      }).click()
    },
    expectMessages(messages: Record<'role' | 'message', string>[]) {
      messages.forEach(({ role, message }) => {
        cy.findByText(message, {
          selector: role === 'user' ? '.card' : '.ai-chat',
        })
      })
    },
  }
}
