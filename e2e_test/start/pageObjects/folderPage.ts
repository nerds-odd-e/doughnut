import { pageIsNotLoading } from '../pageBase'

const folderPage = () => ({
  openSettingsTab() {
    cy.get('[data-testid="folder-workspace-tab-settings"]').click()
    cy.get('[data-testid="folder-workspace-settings"]').should('be.visible')
    return this
  },

  typeFolderIndexDraftAndSave(text: string) {
    cy.get('[data-testid="folder-index-editor"]')
      .scrollIntoView()
      .should('be.visible')
    cy.get('[data-testid="folder-index-editor"] .ql-editor')
      .should('be.visible')
      .click()
      .type(text, { delay: 0 })
      .blur()
    pageIsNotLoading()
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
