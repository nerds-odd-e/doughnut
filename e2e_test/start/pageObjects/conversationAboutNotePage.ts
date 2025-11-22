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

  expectExportContainsUserMessage(message: string) {
    cy.get('[data-testid="export-textarea"]')
      .should(($textarea) => {
        const content = $textarea.val() as string
        expect(content).to.not.be.empty
        expect(content.trim()).to.match(/^\{/)
      })
      .then(($textarea) => {
        const content = $textarea.val() as string
        const json = JSON.parse(content)
        // Handle both old structure (messages array) and new structure (messages array with different format)
        const messages = json.messages || (Array.isArray(json) ? json : [])
        if (!messages || messages.length === 0) {
          throw new Error(
            `No messages found in export. JSON structure: ${JSON.stringify(json, null, 2)}`
          )
        }
        const userMessages = messages.filter((m: any) => {
          // Check if message has user field (new structure) or role field (old structure)
          const isUser = m.user !== undefined || m.role === 'user'
          if (!isUser) return false
          // Extract content from new structure (user.content) or old structure (content)
          const messageContent = m.user?.content || m.content || ''
          const contentStr =
            typeof messageContent === 'string'
              ? messageContent
              : JSON.stringify(messageContent)
          return contentStr.includes(message)
        })
        expect(userMessages.length).to.be.greaterThan(0)
      })
    return this
  }

  expectExportContainsAssistantReply(reply: string) {
    cy.get('[data-testid="export-textarea"]')
      .should(($textarea) => {
        const content = $textarea.val() as string
        expect(content).to.not.be.empty
        expect(content.trim()).to.match(/^\{/)
      })
      .then(($textarea) => {
        const content = $textarea.val() as string
        const json = JSON.parse(content)
        // Handle both old structure (messages array) and new structure (messages array with different format)
        const messages = json.messages || (Array.isArray(json) ? json : [])
        if (!messages || messages.length === 0) {
          throw new Error(
            `No messages found in export. JSON structure: ${JSON.stringify(json, null, 2)}`
          )
        }
        const assistantMessages = messages.filter((m: any) => {
          // Check if message has assistant field (new structure) or role field (old structure)
          const isAssistant =
            m.assistant !== undefined || m.role === 'assistant'
          if (!isAssistant) return false
          // Extract content from new structure (assistant.content) or old structure (content)
          const messageContent = m.assistant?.content || m.content || ''
          const contentStr =
            typeof messageContent === 'string'
              ? messageContent
              : JSON.stringify(messageContent)
          return contentStr.includes(reply)
        })
        expect(assistantMessages.length).to.be.greaterThan(0)
      })
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
