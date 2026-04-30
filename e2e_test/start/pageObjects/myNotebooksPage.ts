/// <reference types="cypress" />
import { pageIsNotLoading } from '../pageBase'
import router from '../router'
import type NotePath from '../../support/NotePath'
import { navigateAlongNotebookCatalogPath } from '../navigateNotePath'
import { notebookCard } from './notebookCard'
import { notebookList } from './NotebookList'
import noteCreationForm from './noteForms/noteCreationForm'
import { subscribedNotebooks } from './subscribedNotebooks'

const addNewNotebookButton = () =>
  cy.findByRole('button', { name: 'Add New Notebook' })

const completeMoveNotebookToNewGroupDialog = (newGroupName: string) => {
  cy.findByRole('dialog', { name: 'Move to group' }).within(() => {
    cy.get('#notebook-catalog-move-to-group-target').select('new')
    cy.findByLabelText('New group name').type(newGroupName)
    cy.findByRole('button', { name: 'Move' }).click()
  })
  pageIsNotLoading()
}

const completeMoveNotebookToUngroupedDialog = () => {
  cy.findByRole('dialog', { name: 'Move to group' }).within(() => {
    cy.get('#notebook-catalog-move-to-group-target').select('ungrouped')
    cy.findByRole('button', { name: 'Move' }).click()
  })
  pageIsNotLoading()
}

const myNotebooksPage = () => {
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
    creatingNotebook(notebookTopic: string, description?: string) {
      addNewNotebookButton().click()
      if (description !== undefined) {
        return noteCreationForm.createNotebookWithNameAndDescription(
          notebookTopic,
          description
        )
      }
      return noteCreationForm.createNoteWithTitle(notebookTopic)
    },
    notebookCard(notebook: string) {
      return notebookCard(notebook)
    },
    subscribedNotebooks() {
      return subscribedNotebooks()
    },
    expectNotebookNotToExist(notebookName: string) {
      cy.get('main').within(() =>
        cy
          .findByText(notebookName, { selector: '.notebook-card h5' })
          .should('not.exist')
      )
    },
    expectNotebookToExist(notebookName: string) {
      cy.get('main').within(() =>
        cy
          .findByText(notebookName, { selector: '.notebook-card h5' })
          .should('exist')
      )
    },
    creatingNotebookGroupFromCatalogMove(
      notebookName: string,
      groupName: string,
      isSubscribed?: boolean
    ) {
      if (isSubscribed) {
        subscribedNotebooks().card(notebookName).openMoveToGroupDialog()
      } else {
        notebookCard(notebookName).openMoveToGroupDialog()
      }
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
        'contain.text',
        hintSubstring
      )
      return this as any
    },
    openNotebookGroupFromHeader(groupName: string) {
      cy.contains('[data-cy="notebook-group-card"]', groupName)
        .find('[data-cy="notebook-group-header-link"]')
        .click()
      pageIsNotLoading()
      return this as any
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
  router().push('/d/notebooks', 'notebooks', {})
  cy.get('.loading-bar').should('not.exist', { timeout: 30000 })
  return myNotebooksPage()
}

export const navigateToNotebookPage = (notebookName: string) =>
  navigateToNotebooksPage().notebookCard(notebookName).openNotebookPage()
