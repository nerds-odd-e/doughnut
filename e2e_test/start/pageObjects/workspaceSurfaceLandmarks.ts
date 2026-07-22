/**
 * Cross-route main-column landmarks that distinguish note, folder, and notebook.
 */
import folderPage from './folderPage'

const workspaceSurfaceLandmarks = () => ({
  expectNotebookWorkspaceTabsPresent() {
    cy.get('[data-testid="notebook-page-kind-label"]').should(
      'contain.text',
      'Notebook'
    )
    cy.get('[data-testid="notebook-workspace-tabs"]').should('be.visible')
    cy.get('[data-testid="notebook-workspace-tab-readme"]').should('be.visible')
    cy.get('[data-testid="notebook-workspace-tab-settings"]').should(
      'be.visible'
    )
    return this
  },

  expectNotebookWorkspaceTabsAbsent() {
    cy.get('[data-testid="notebook-page-kind-label"]').should('not.exist')
    cy.get('[data-testid="notebook-workspace-tabs"]').should('not.exist')
    cy.get('[data-testid="notebook-workspace-tab-readme"]').should('not.exist')
    cy.get('[data-testid="notebook-workspace-tab-settings"]').should(
      'not.exist'
    )
    return this
  },

  expectFolderWorkspaceTabsPresent() {
    cy.get('[data-testid="folder-page-kind-label"]').should(
      'contain.text',
      'Folder'
    )
    cy.get('[data-testid="folder-workspace-tabs"]').should('be.visible')
    cy.get('[data-testid="folder-workspace-tab-readme"]').should('be.visible')
    cy.get('[data-testid="folder-workspace-tab-settings"]').should('be.visible')
    return this
  },

  expectFolderWorkspaceTabsAbsent() {
    cy.get('[data-testid="folder-page-kind-label"]').should('not.exist')
    cy.get('[data-testid="folder-workspace-tabs"]').should('not.exist')
    return this
  },

  openFolderSettingsTab() {
    folderPage().openSettingsTab()
    return this
  },

  expectFolderAdminControlsPresent() {
    cy.get('[data-testid="folder-readme-editor"]').should('be.visible')
    this.openFolderSettingsTab()
    cy.get('[data-testid="folder-move-dialog"]').should('be.visible')
    cy.get('[data-testid="folder-rename-submit"]').should('exist')
    cy.get('[data-testid="folder-dissolve-button"]').should('exist')
    return this
  },

  expectFolderAdminControlsAbsent() {
    cy.get('[data-testid="folder-move-dialog"]').should('not.exist')
    cy.get('[data-testid="folder-rename-submit"]').should('not.exist')
    cy.get('[data-testid="folder-dissolve-button"]').should('not.exist')
    return this
  },

  expectNoteDocumentToolbarPresent() {
    cy.get('#main-note-content', { timeout: 15000 }).should('be.visible')
    cy.get('[data-note-toolbar]', { timeout: 15000 }).should('be.visible')
    return this
  },

  expectNoteDocumentToolbarAbsent() {
    cy.get('[data-note-toolbar]').should('not.exist')
    return this
  },
})

export default workspaceSurfaceLandmarks
