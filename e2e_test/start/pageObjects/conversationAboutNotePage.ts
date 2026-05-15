function flattenExportContent(content: unknown): string {
  if (typeof content === 'string') return content
  if (Array.isArray(content)) {
    return content
      .map((p: { type?: string; text?: string }) => {
        if (p?.type === 'input_text' || p?.type === 'text') return p.text ?? ''
        return ''
      })
      .join('')
  }
  return ''
}

function exportMessagesFromJson(json: Record<string, unknown>): Array<{
  role: string
  content: string
}> {
  const legacy = json.messages
  if (Array.isArray(legacy) && legacy.length > 0) {
    return legacy as Array<{ role: string; content: string }>
  }
  const input = json.input as unknown
  const inputList = Array.isArray(input)
    ? input
    : input &&
        typeof input === 'object' &&
        Array.isArray((input as { response?: unknown[] }).response)
      ? (input as { response: unknown[] }).response
      : []
  if (!Array.isArray(inputList)) {
    return []
  }
  return inputList
    .filter(
      (m) =>
        m &&
        typeof m === 'object' &&
        typeof (m as { role?: string }).role === 'string'
    )
    .map((m) => {
      const o = m as { role: string; content?: unknown }
      return { role: o.role, content: flattenExportContent(o.content) }
    })
}

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

  shouldShowCompletion() {
    cy.findByRole('dialog')
      .should('be.visible')
      .within(() => {
        // Check that the completion dialog contains diff content
        cy.get('.completion-text').should('be.visible')
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
    cy.get('.status-bar').should('not.exist')
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
        const json = JSON.parse(content) as Record<string, unknown>
        const messages = exportMessagesFromJson(json)
        if (!messages || messages.length === 0) {
          throw new Error(
            `No messages found in export. JSON structure: ${JSON.stringify(json, null, 2)}`
          )
        }
        const userMessages = messages.filter((m) => {
          const isUser = m.role === 'user'
          if (!isUser) return false
          const messageContent = m.content || ''
          return messageContent.includes(message)
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
        const json = JSON.parse(content) as Record<string, unknown>
        const messages = exportMessagesFromJson(json)
        if (!messages || messages.length === 0) {
          throw new Error(
            `No messages found in export. JSON structure: ${JSON.stringify(json, null, 2)}`
          )
        }
        const assistantMessages = messages.filter((m) => {
          const isAssistant = m.role === 'assistant'
          if (!isAssistant) return false
          const messageContent = m.content || ''
          return messageContent.includes(reply)
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
