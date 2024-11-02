export function assumeChatAboutNotePage() {
  return {
    replyToConversation(msg: string) {
      cy.focused().type(msg)
      cy.get('#chat-button').click()
    },
    replyToConversationAndInviteAiToReply(msg: string) {
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
