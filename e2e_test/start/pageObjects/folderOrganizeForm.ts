import {
  clickPopupConfirmOk,
  declineMergeConfirmIfShown,
} from '../../support/daisyModalHelpers'
import { waitUntilAppIsNotBusy } from '../pageBase'

const submitTimeoutMs = 20000

export type FolderOrganizeForm = {
  selectDestinationNotebook: (notebookName: string) => FolderOrganizeForm
  selectNotebookRootAsDestination: () => FolderOrganizeForm
  selectFolderDestinationByName: (folderName: string) => FolderOrganizeForm
  confirmMove: () => void
  tryConfirmMove: () => FolderOrganizeForm
  confirmMerge: () => void
  expectErrorText: (text: string) => FolderOrganizeForm
  dissolveFolder: () => void
  dissolveFolderWithMerge: () => void
  trashFolder: () => void
  permanentlyDeleteFolder: () => void
}

/**
 * Page object for folder move / dissolve on the folder page (`data-testid="folder-move-dialog"`).
 */
export function assumeFolderOrganizeForm(): FolderOrganizeForm {
  return {
    selectDestinationNotebook(notebookName: string) {
      cy.get('[data-testid="folder-move-notebook-select"]').select(notebookName)
      return assumeFolderOrganizeForm()
    },

    selectNotebookRootAsDestination() {
      cy.get('[data-testid="folder-move-parent-select"]').select('__root__')
      return assumeFolderOrganizeForm()
    },

    selectFolderDestinationByName(folderName: string) {
      cy.get('[data-testid="folder-selector-more-button"]').click()
      cy.get('[data-testid="folder-selector-search-dialog"]').should(
        'be.visible'
      )
      cy.get('[data-testid="folder-selector-search-input"]')
        .clear()
        .type(folderName)
      cy.contains(
        '[data-testid="folder-selector-search-result"]',
        folderName
      ).click()
      return assumeFolderOrganizeForm()
    },

    confirmMove() {
      cy.get('[data-testid="folder-move-submit"]', { timeout: submitTimeoutMs })
        .should('not.be.disabled')
        .click()
      waitUntilAppIsNotBusy()
    },

    tryConfirmMove() {
      cy.get('[data-testid="folder-move-submit"]', { timeout: submitTimeoutMs })
        .should('not.be.disabled')
        .click()
      declineMergeConfirmIfShown()
      return assumeFolderOrganizeForm()
    },

    confirmMerge() {
      cy.get('[data-testid="folder-move-submit"]', { timeout: submitTimeoutMs })
        .should('not.be.disabled')
        .click()
      clickPopupConfirmOk()
      waitUntilAppIsNotBusy()
    },

    expectErrorText(text: string) {
      cy.get('[data-testid="folder-move-dialog"]')
        .find('.text-error')
        .should(($el) => {
          const actual = $el.text()
          expect(
            actual,
            `Expected folder page error to contain "${text}", but found "${actual}"`
          ).to.include(text)
        })
      return assumeFolderOrganizeForm()
    },

    dissolveFolder() {
      cy.get('[data-testid="folder-dissolve-button"]', {
        timeout: submitTimeoutMs,
      })
        .should('not.be.disabled')
        .click()
      clickPopupConfirmOk()
      waitUntilAppIsNotBusy()
    },

    dissolveFolderWithMerge() {
      cy.get('[data-testid="folder-dissolve-button"]', {
        timeout: submitTimeoutMs,
      })
        .should('not.be.disabled')
        .click()
      clickPopupConfirmOk()
      clickPopupConfirmOk()
      waitUntilAppIsNotBusy()
    },

    trashFolder() {
      cy.get('[data-testid="folder-trash-button"]', {
        timeout: submitTimeoutMs,
      })
        .should('not.be.disabled')
        .click()
      clickPopupConfirmOk()
      waitUntilAppIsNotBusy()
    },

    permanentlyDeleteFolder() {
      cy.get('[data-testid="folder-permanent-delete-button"]', {
        timeout: submitTimeoutMs,
      })
        .should('not.be.disabled')
        .click()
      clickPopupConfirmOk()
      waitUntilAppIsNotBusy()
    },
  }
}
