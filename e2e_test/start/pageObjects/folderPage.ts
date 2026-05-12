import { pageIsNotLoading } from '../pageBase'

const folderPage = () => ({
  typeFolderIndexDraftAndSave(text: string) {
    cy.get('[data-testid="folder-index-editor"] .ql-editor')
      .should('be.visible')
      .click()
      .type(text, { delay: 0 })
    cy.get('[data-testid="folder-index-editor"] .ql-editor').should(
      'contain.text',
      text
    )
    cy.get('[data-testid="folder-index-save"]').click()
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
