import { pageIsNotLoading } from '../pageBase'

const folderPage = () => ({
  typeFolderIndexDraftAndSave(text: string) {
    cy.get('[data-testid="folder-index-draft-editor"] .ql-editor')
      .should('be.visible')
      .click()
    cy.focused().type(text, { delay: 0 })
    cy.get('[data-testid="folder-index-create-save"]').click()
    cy.get('[data-testid="folder-index-create-save"]').should('not.exist')
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
