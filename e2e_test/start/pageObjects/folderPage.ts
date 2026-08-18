import { waitUntilAppIsNotBusy } from '../pageBase'
import { assumeFolderOrganizeForm } from './folderOrganizeForm'

const folderPage = () => ({
  renameHeading(name: string) {
    cy.get('[data-test="folder-page-name"]')
      .should('be.visible')
      .clear()
      .type(name, { delay: 0 })
      .blur()
    waitUntilAppIsNotBusy()
    return this
  },

  reload() {
    cy.reload()
    waitUntilAppIsNotBusy()
    return this
  },

  expectHeading(name: string) {
    cy.get('[data-test="folder-page-name"]').should(($heading) => {
      const actual = $heading.text().trim()
      expect(
        actual,
        `Expected folder page heading "${name}", but found "${actual}"`
      ).to.equal(name)
    })
    return this
  },

  expectNoOkfIncompatibleTitleWarning() {
    waitUntilAppIsNotBusy()
    cy.get('[data-testid="folder-page-summary"] .path-name-editor').should(
      ($el) => {
        const actual = $el.text().trim()
        expect(
          actual,
          `Expected no OKF-incompatible warning on the folder name, but found: "${actual}"`
        ).not.to.match(/OKF-incompatible/i)
      }
    )
    return this
  },

  openSettingsTab() {
    cy.get('[data-testid="folder-tab-settings"]').click()
    cy.get('[data-testid="folder-settings"]').should('be.visible')
    return this
  },

  openOrganizeForm() {
    this.openSettingsTab()
    return assumeFolderOrganizeForm()
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
