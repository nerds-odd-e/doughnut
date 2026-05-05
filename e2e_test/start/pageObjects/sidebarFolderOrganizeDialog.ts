import { pageIsNotLoading } from '../pageBase'

const submitTimeoutMs = 20000

export type SidebarFolderOrganizeDialog = {
  selectNotebookRootAsDestination: () => SidebarFolderOrganizeDialog
  confirmMove: () => void
}

/**
 * Page object for the folder move dialog (opened from the sidebar toolbar Folder… control).
 */
export function assumeSidebarFolderOrganizeDialog(): SidebarFolderOrganizeDialog {
  return {
    selectNotebookRootAsDestination() {
      cy.get('[data-testid="folder-move-parent-select"]').select('__root__')
      return assumeSidebarFolderOrganizeDialog()
    },

    confirmMove() {
      cy.get('[data-testid="folder-move-submit"]', { timeout: submitTimeoutMs })
        .should('not.be.disabled')
        .click()
      pageIsNotLoading()
    },
  }
}
