/// <reference types="cypress" />
import { waitUntilAppIsNotBusy } from '../pageBase'
import type NotePath from '../../support/NotePath'
import {
  navigateAlongNotebookCatalogPath,
  openFolderAlongNotebookCatalogPath,
} from '../navigateNotePath'
import { noteSidebar } from './noteSidebar'
import { notebookCard } from './notebookCard'
import { notebookList } from './NotebookList'
import { subscribedNotebooks } from './subscribedNotebooks'
import router from 'start/router'
import notebookCreationForm from './forms/notebookCreationForm'
import notebookGroupPage from './notebookGroupPage'
import {
  completeMoveNotebookToNewGroupDialog,
  completeMoveNotebookToUngroupedDialog,
} from './notebookCatalogMoveToGroup'

const addNewNotebookButton = () =>
  cy.findByRole('button', { name: 'Add New Notebook' })

export const myNotebooksPage = () => {
  cy.contains('h1', 'My notebooks', { timeout: 15000 }).should('be.visible')

  return {
    ...notebookList(),
    navigateToPath(notePath: NotePath) {
      const segments = notePath.path
      if (segments.length === 0) {
        return this as any
      }
      return navigateAlongNotebookCatalogPath(segments) as any
    },
    /**
     * Walks the catalog sidebar treating every path segment after the notebook as structural
     * (folders). Unlike {@link navigateToPath}, does not open the last segment as a note.
     */
    expandFolderInSidebar(notePath: NotePath) {
      const segments = notePath.path
      if (segments.length < 2) {
        throw new Error(
          'openFolder requires a notebook plus at least one path segment (folder path)'
        )
      }
      openFolderAlongNotebookCatalogPath(segments)
      const folderLabel = segments[segments.length - 1]!
      return {
        expectChildrenUnderSidebarFolder(children: Record<string, string>[]) {
          noteSidebar().expectChildrenUnderFolder(folderLabel, children)
          return this as any
        },
      }
    },
    creatingNotebook(notebookTopic: string, description?: string) {
      addNewNotebookButton().click()
      return notebookCreationForm.createNotebookWithNameAndDescription(
        notebookTopic,
        description
      )
    },
    notebookCard(notebook: string) {
      return notebookCard(notebook)
    },
    subscribedNotebooks() {
      return subscribedNotebooks()
    },
    creatingNotebookGroupFromOwnedCatalogMove(
      notebookName: string,
      groupName: string
    ) {
      notebookCard(notebookName).openMoveToGroupDialog()
      completeMoveNotebookToNewGroupDialog(groupName)
      return this as any
    },
    creatingNotebookGroupFromSubscribedCatalogMove(
      notebookName: string,
      groupName: string
    ) {
      subscribedNotebooks().card(notebookName).openMoveToGroupDialog()
      completeMoveNotebookToNewGroupDialog(groupName)
      return this as any
    },
    moveOwnedNotebookToUngrouped(notebookName: string) {
      notebookCard(notebookName).openMoveToGroupDialog()
      completeMoveNotebookToUngroupedDialog()
      return this as any
    },
    expectNotebookGroupWithMemberHint(
      groupName: string,
      hintSubstring: string
    ) {
      cy.contains('[data-cy="notebook-group-card"]', groupName).should(
        ($card) => {
          const actual = $card.text()
          expect(
            actual,
            `Expected notebook group "${groupName}" hint to include "${hintSubstring}", but found "${actual.trim()}"`
          ).to.include(hintSubstring)
        }
      )
      return this as any
    },
    openNotebookGroupFromHeader(groupName: string) {
      cy.contains('[data-cy="notebook-group-card"]', groupName)
        .find('[data-cy="notebook-group-header-link"]')
        .click()
      waitUntilAppIsNotBusy()
      return notebookGroupPage()
    },
    addingNotebookToGroupFromCatalog(groupName: string, notebookName: string) {
      cy.contains('[data-cy="notebook-group-card"]', groupName)
        .find('[data-cy="notebook-group-overflow"]')
        .click()
      cy.findByTestId('notebook-group-add-notebook').click()
      cy.findByRole('dialog', { name: 'New notebook' }).within(() => {
        cy.findByTestId('notebook-new-form-group-hint').should(
          'contain.text',
          `Creates in group "${groupName}".`
        )
      })
      notebookCreationForm.createNotebookWithNameAndDescription(notebookName)
      waitUntilAppIsNotBusy()
      router().push('/notebooks', 'notebooks', {})
      waitUntilAppIsNotBusy()
      return myNotebooksPage()
    },
    expectNotebookAtTopLevelOfCatalog(notebookName: string) {
      cy.get('.notebook-catalog-section--list > [data-cy="notebook-card"]')
        .contains('h5', notebookName)
        .should('be.visible')
      return this as any
    },
  }
}

export const navigateToNotebooksPage = () => {
  router().push('/notebooks', 'notebooks', {})
  waitUntilAppIsNotBusy()
  return myNotebooksPage()
}

export const navigateToNotebookPage = (notebookName: string) =>
  navigateToNotebooksPage().notebookCard(notebookName).openNotebookPage()
