import { waitUntilAppIsNotBusy } from '../pageBase'

const folderPage = () => ({
  openSettingsTab() {
    cy.get('[data-testid="folder-tab-settings"]').click()
    cy.get('[data-testid="folder-settings"]').should('be.visible')
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
    cy.get('[data-testid="folder-readme-body"]').should(
      'not.have.class',
      'dirty'
    )
    waitUntilAppIsNotBusy()
    return this
  },
  expectFolderReadmeBodyContains(fragment: string) {
    waitUntilAppIsNotBusy()
    cy.get('[data-testid="folder-readme-body"]')
      .scrollIntoView()
      .should('be.visible')
    cy.get('[data-testid="folder-readme-body"] .ql-editor').should(($el) => {
      const actual = $el.text()
      expect(
        actual,
        `Expected folder readme to contain "${fragment}", but found "${actual}"`
      ).to.include(fragment)
    })
    return this
  },
})

export default folderPage
