import { pageIsNotLoading } from '../pageBase'

const submitTimeoutMs = 20000

export type SidebarFolderOrganizeForm = {
  selectNotebookRootAsDestination: () => SidebarFolderOrganizeForm
  openFolderSearch: () => SidebarFolderOrganizeForm
  searchFolderDestination: (text: string) => SidebarFolderOrganizeForm
  selectFolderSearchResultByName: (
    folderName: string
  ) => SidebarFolderOrganizeForm
  confirmMove: () => void
  tryConfirmMove: () => SidebarFolderOrganizeForm
  expectErrorText: (text: string) => SidebarFolderOrganizeForm
  dissolveFolder: () => void
}

/**
 * Page object for the folder move dialog (opened from the sidebar toolbar Folder… control).
 */
export function assumeSidebarFolderOrganizeForm(): SidebarFolderOrganizeForm {
  return {
    selectNotebookRootAsDestination() {
      cy.get('[data-testid="folder-move-parent-select"]').select('__root__')
      return assumeSidebarFolderOrganizeForm()
    },

    openFolderSearch() {
      cy.get('[data-testid="folder-selector-more-button"]').click()
      cy.get('[data-testid="folder-selector-search-dialog"]').should(
        'be.visible'
      )
      return assumeSidebarFolderOrganizeForm()
    },

    searchFolderDestination(text: string) {
      cy.get('[data-testid="folder-selector-search-input"]').clear().type(text)
      return assumeSidebarFolderOrganizeForm()
    },

    selectFolderSearchResultByName(folderName: string) {
      cy.contains(
        '[data-testid="folder-selector-search-result"]',
        folderName
      ).click()
      return assumeSidebarFolderOrganizeForm()
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
      return assumeSidebarFolderOrganizeForm()
    },

    expectErrorText(text: string) {
      cy.get('[data-testid="folder-move-dialog"]')
        .find('.daisy-text-error')
        .should('contain.text', text)
      return assumeSidebarFolderOrganizeForm()
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
