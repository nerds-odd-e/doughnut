export class ConversationAboutNotePage {
  replyToConversation(msg: string) {
    cy.focused().type(msg)
    cy.get('#chat-button').click()
    return this
  }

  replyToConversationAndInviteAiToReply(msg: string) {
    cy.focused().type(msg)
    cy.findByRole('button', {
      name: 'Send message and invite AI to reply',
    }).click()
    return this
  }

  expectMessages(messages: Record<'role' | 'message', string>[]) {
    messages.forEach(({ role, message }) => {
      cy.findByText(message, {
        selector: role === 'user' ? 'pre' : '.ai-chat *',
      })
    })
    return this
  }

  expectErrorMessage(message: string) {
    cy.get('.last-error-message').should('have.text', message)
    return this
  }

  shouldShowCompletion(completion: string) {
    cy.findByRole('dialog')
      .should('be.visible')
      .within(() => {
        cy.findByText(completion)
      })
    return this
  }

  acceptCompletion() {
    cy.findByRole('dialog').within(() => {
      cy.findByRole('button', { name: 'Accept' }).click()
    })
    return this
  }

  cancelCompletion() {
    cy.findByRole('dialog').within(() => {
      cy.findByRole('button', { name: 'Cancel' }).click()
    })
    return this
  }
}

export const assumeConversationAboutNotePage = () =>
  new ConversationAboutNotePage()
