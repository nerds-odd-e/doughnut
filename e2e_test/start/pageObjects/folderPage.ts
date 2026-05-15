import { pageIsNotLoading } from '../pageBase'

const folderPage = () => ({
  typeFolderIndexDraftAndSave(text: string) {
    cy.get('[data-testid="folder-index-editor"]')
      .scrollIntoView()
      .should('be.visible')
    cy.get('[data-testid="folder-index-editor"] .ql-editor')
      .should('be.visible')
      .click()
      .type(text, { delay: 0 })
    cy.get('[data-testid="folder-index-save"]').click()
    pageIsNotLoading()
    cy.contains('.Vue-Toastification__toast--success', 'Folder index saved', {
      timeout: 10000,
    }).should('be.visible')
    return this
  },
  expectFolderIndexBodyContains(fragment: string) {
    pageIsNotLoading()
    cy.get('[data-testid="folder-index-body"]')
      .scrollIntoView()
      .should('be.visible')
    cy.get('[data-testid="folder-index-body"] .ql-editor').should(
      'contain.text',
      fragment
    )
    return this
  },
})

export default folderPage
