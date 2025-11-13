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

  exportConversation() {
    cy.findByRole('button', { name: 'Export conversation' }).click()
    return this
  }

  expectExportContainsTitle(title: string) {
    cy.get('[data-testid="export-textarea"]').should(
      'contain.value',
      `# Conversation: ${title}`
    )
    return this
  }

  expectExportContainsUserMessage(message: string) {
    cy.get('[data-testid="export-textarea"]').should(
      'contain.value',
      `**User**: ${message}`
    )
    return this
  }

  expectExportContainsAssistantReply(reply: string) {
    cy.get('[data-testid="export-textarea"]').should(
      'contain.value',
      `**Assistant**: ${reply}`
    )
    return this
  }

  copyExport() {
    cy.get('[data-testid="copy-export-btn"]').click()
    cy.get('[data-testid="copy-export-btn"]').within(() => {
      cy.get('svg').should('exist')
    })
    return this
  }
}

export const assumeConversationAboutNotePage = () =>
  new ConversationAboutNotePage()
