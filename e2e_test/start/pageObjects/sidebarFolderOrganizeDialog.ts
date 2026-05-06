import { pageIsNotLoading } from '../pageBase'

const submitTimeoutMs = 20000

export type SidebarFolderOrganizeDialog = {
  selectNotebookRootAsDestination: () => SidebarFolderOrganizeDialog
  confirmMove: () => void
  tryConfirmMove: () => SidebarFolderOrganizeDialog
  expectErrorText: (text: string) => SidebarFolderOrganizeDialog
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

    tryConfirmMove() {
      cy.get('[data-testid="folder-move-submit"]', { timeout: submitTimeoutMs })
        .should('not.be.disabled')
        .click()
      return assumeSidebarFolderOrganizeDialog()
    },

    expectErrorText(text: string) {
      cy.get('[data-testid="folder-move-dialog"]')
        .find('.daisy-text-error')
        .should('contain.text', text)
      return assumeSidebarFolderOrganizeDialog()
    },
  }
}
