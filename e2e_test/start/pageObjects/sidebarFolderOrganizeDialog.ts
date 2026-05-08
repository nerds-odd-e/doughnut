import { pageIsNotLoading } from '../pageBase'

const submitTimeoutMs = 20000

export type SidebarFolderOrganizeDialog = {
  selectNotebookRootAsDestination: () => SidebarFolderOrganizeDialog
  openFolderSearch: () => SidebarFolderOrganizeDialog
  searchFolderDestination: (text: string) => SidebarFolderOrganizeDialog
  selectFolderSearchResultByName: (
    folderName: string
  ) => SidebarFolderOrganizeDialog
  confirmMove: () => void
  tryConfirmMove: () => SidebarFolderOrganizeDialog
  expectErrorText: (text: string) => SidebarFolderOrganizeDialog
  dissolveFolder: () => void
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

    openFolderSearch() {
      cy.get('[data-testid="folder-selector-more-button"]').click()
      cy.get('[data-testid="folder-selector-search-dialog"]').should(
        'be.visible'
      )
      return assumeSidebarFolderOrganizeDialog()
    },

    searchFolderDestination(text: string) {
      cy.get('[data-testid="folder-selector-search-input"]').clear().type(text)
      return assumeSidebarFolderOrganizeDialog()
    },

    selectFolderSearchResultByName(folderName: string) {
      cy.contains(
        '[data-testid="folder-selector-search-result"]',
        folderName
      ).click()
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

    dissolveFolder() {
      cy.get('[data-testid="folder-dissolve-button"]', {
        timeout: submitTimeoutMs,
      })
        .should('not.be.disabled')
        .click()
      cy.findByRole('button', { name: 'OK' }).click()
      pageIsNotLoading()
    },
  }
}
