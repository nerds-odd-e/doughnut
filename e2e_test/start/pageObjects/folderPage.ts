import { pageIsNotLoading } from '../pageBase'

function escapeCypressTypeBraces(s: string): string {
  return s.replace(/\{/g, '{{').replace(/\}/g, '}}')
}

const folderPage = () => ({
  typeFolderIndexDraftAndSave(text: string) {
    cy.get('[data-testid="folder-index-draft-editor"] .ql-editor')
      .should('be.visible')
      .click()
    cy.focused().type(text, { delay: 0 })
    cy.get('[data-testid="folder-index-create-save"]').click()
    pageIsNotLoading()
    return this
  },

  typeFolderIndexDraftMarkdownAndSave(markdown: string) {
    const normalized = markdown.replace(/\r\n/g, '\n')
    const lines = normalized.split('\n')
    cy.get('[data-testid="folder-index-draft-editor"] .ql-editor')
      .should('be.visible')
      .click()
    cy.focused().type(escapeCypressTypeBraces(lines[0]!), { delay: 0 })
    for (let i = 1; i < lines.length; i++) {
      cy.focused().type(`{enter}${escapeCypressTypeBraces(lines[i]!)}`, {
        delay: 0,
      })
    }
    cy.get('[data-testid="folder-index-create-save"]').click()
    pageIsNotLoading()
    return this
  },
  expectFolderIndexBodyContains(fragment: string) {
    pageIsNotLoading()
    cy.get('[data-testid="folder-index-body"] .ql-editor').should(
      'contain.text',
      fragment
    )
    return this
  },
})

export default folderPage
