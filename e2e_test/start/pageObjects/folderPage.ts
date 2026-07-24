import { waitUntilAppIsNotBusy } from '../pageBase'

const folderPage = () => ({
  openSettingsTab() {
    cy.get('[data-testid="folder-workspace-tab-settings"]').click()
    cy.get('[data-testid="folder-workspace-settings"]').should('be.visible')
    return this
  },

  typeFolderReadmeDraftAndSave(text: string) {
    cy.get('[data-testid="folder-readme-editor"]')
      .scrollIntoView()
      .should('be.visible')
    cy.get('[data-testid="folder-readme-editor"] .ql-editor')
      .should('be.visible')
      .click()
      .type(text, { delay: 0 })
      .blur()
    waitUntilAppIsNotBusy()
    return this
  },
  expectFolderReadmeBodyContains(fragment: string) {
    waitUntilAppIsNotBusy()
    cy.get('[data-testid="folder-readme-body"]')
      .scrollIntoView()
      .should('be.visible')
    cy.get('[data-testid="folder-readme-body"] .ql-editor').should(
      'contain.text',
      fragment
    )
    return this
  },
})

export default folderPage
