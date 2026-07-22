/**
 * Cross-route main-column landmarks that distinguish note, folder, and notebook.
 */
const workspaceSurfaceLandmarks = () => ({
  expectNotebookWorkspaceTabsPresent() {
    cy.get('[data-testid="notebook-workspace-tabs"]').should('be.visible')
    cy.get('[data-testid="notebook-workspace-tab-home"]').should('be.visible')
    cy.get('[data-testid="notebook-workspace-tab-settings"]').should(
      'be.visible'
    )
    return this
  },

  expectNotebookWorkspaceTabsAbsent() {
    cy.get('[data-testid="notebook-workspace-tabs"]').should('not.exist')
    cy.get('[data-testid="notebook-workspace-tab-home"]').should('not.exist')
    cy.get('[data-testid="notebook-workspace-tab-settings"]').should(
      'not.exist'
    )
    return this
  },

  expectFolderAdminControlsPresent() {
    cy.get('[data-testid="folder-index-editor"]').should('be.visible')
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
