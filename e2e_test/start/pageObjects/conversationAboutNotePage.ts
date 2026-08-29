import { waitUntilAppIsNotBusy } from '../pageBase'

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
  replyToConversationAndInviteAiToReply(msg: string) {
    cy.focused().type(msg, { delay: 0 })
    cy.findByRole('button', {
      name: 'Send message and invite AI to reply',
    }).click()
    waitUntilAppIsNotBusy()
    return this
  }

  expectMessages(messages: Record<'role' | 'message', string>[]) {
    messages.forEach(({ role, message }) => {
      const selector = role === 'user' ? 'pre' : '.ai-assistant *'
      cy.findByText(message, { selector }).should(($el) => {
        const actual = $el.text().trim()
        expect(
          actual,
          `Expected ${role} message "${message}", but found "${actual}"`
        ).to.equal(message)
      })
    })
    return this
  }

  expectErrorMessage(message: string) {
    cy.get('.last-error-message').should('have.text', message)
    return this
  }

  closeConversation() {
    cy.findByRole('button', { name: 'Close dialog' }).click()
    waitUntilAppIsNotBusy()
    return this
  }

  shouldShowCompletion() {
    cy.findByRole('dialog')
      .should('be.visible')
      .within(() => {
        cy.get('.completion-text').should(($el) => {
          expect(
            $el.is(':visible'),
            'Expected suggested note content completion to be visible'
          ).to.be.true
        })
      })
    return this
  }

  acceptCompletion() {
    cy.findByRole('dialog').within(() => {
      cy.findByRole('button', { name: 'Accept' }).click()
    })
    waitUntilAppIsNotBusy()
    return this
  }

  exportConversation() {
    cy.get('.status-bar').should('not.exist')
    cy.findByRole('button', { name: 'Export conversation' }).click()
    return this
  }

  expectExportContainsUserMessage(message: string) {
    this.expectExportContainsRoleMessage('user', message)
    return this
  }

  expectExportContainsAssistantReply(reply: string) {
    this.expectExportContainsRoleMessage('assistant', reply)
    return this
  }

  private expectExportContainsRoleMessage(role: string, expected: string) {
    cy.get('[data-testid="export-textarea"]')
      .should(($textarea) => {
        const content = $textarea.val() as string
        expect(content, 'export JSON should not be empty').to.not.be.empty
        expect(content.trim(), 'export should be JSON').to.match(/^\{/)
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
        const matching = messages.filter(
          (m) => m.role === role && (m.content || '').includes(expected)
        )
        expect(
          matching.length,
          `Expected export to include ${role} message containing "${expected}", but found roles: ${messages
            .map((m) => `${m.role}:${m.content}`)
            .join(' | ')}`
        ).to.be.greaterThan(0)
      })
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
