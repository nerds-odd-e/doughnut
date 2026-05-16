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
  confirmMerge: () => void
  expectErrorText: (text: string) => SidebarFolderOrganizeForm
  dissolveFolder: () => void
  dissolveFolderWithMerge: () => void
}

/**
 * Page object for folder move / rename / dissolve on the folder page (`data-testid="folder-move-dialog"`).
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
      cy.get('body', { timeout: 8000 }).then(($body) => {
        if ($body.text().includes('Merge into it?')) {
          cy.get('dialog')
            .filter(':visible')
            .findByRole('button', { name: 'Cancel' })
            .click()
        }
      })
      return assumeSidebarFolderOrganizeForm()
    },

    confirmMerge() {
      cy.get('[data-testid="folder-move-submit"]', { timeout: submitTimeoutMs })
        .should('not.be.disabled')
        .click()
      cy.findByRole('button', { name: 'OK' }).click()
      pageIsNotLoading()
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

    dissolveFolderWithMerge() {
      cy.get('[data-testid="folder-dissolve-button"]', {
        timeout: submitTimeoutMs,
      })
        .should('not.be.disabled')
        .click()
      cy.findByRole('button', { name: 'OK' }).click()
      cy.findByRole('button', { name: 'OK' }).click()
      pageIsNotLoading()
    },
  }
}
