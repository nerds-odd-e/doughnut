/// <reference types="cypress" />
import { pageIsNotLoading } from '../pageBase'
import router from '../router'
import type NotePath from '../../support/NotePath'
import type { assumeNotePage } from './notePage'
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

const myNotebooksPage = () => {
  cy.contains('h1', 'My notebooks').should('be.visible')

  return {
    ...notebookList(),
    navigateToPath(notePath: NotePath) {
      return notePath.path.reduce<ReturnType<typeof assumeNotePage>>(
        (page, noteTopology) => page.navigateToChild(noteTopology),
        this as any
      )
    },
    creatingNotebook(notebookTopic: string) {
      addNewNotebookButton().click()
      return noteCreationForm.createNoteWithTitle(notebookTopic)
    },
    notebookCard(notebook: string) {
      return notebookCard(notebook)
    },
    subscribedNotebooks() {
      return subscribedNotebooks()
    },
    expectNotebookNotToExist(notebookTitle: string) {
      cy.get('main').within(() =>
        cy
          .findByText(notebookTitle, { selector: '.notebook-card h5' })
          .should('not.exist')
      )
    },
    expectNotebookToExist(notebookTitle: string) {
      cy.get('main').within(() =>
        cy
          .findByText(notebookTitle, { selector: '.notebook-card h5' })
          .should('exist')
      )
    },
    creatingNotebookGroupFromCatalogMove(
      notebookTitle: string,
      groupName: string,
      isSubscribed?: boolean
    ) {
      if (isSubscribed) {
        subscribedNotebooks().card(notebookTitle).openMoveToGroupDialog()
      } else {
        notebookCard(notebookTitle).openMoveToGroupDialog()
      }
      completeMoveNotebookToNewGroupDialog(groupName)
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
    expectNotebookAtTopLevelOfCatalog(notebookTitle: string) {
      cy.get('.notebook-catalog-section--list > [data-cy="notebook-card"]')
        .contains('h5', notebookTitle)
        .should('be.visible')
      return this as any
    },
  }
}

export const navigateToNotebooksPage = () => {
  pageIsNotLoading()
  router().push('/d/notebooks', 'notebooks', {})
  return myNotebooksPage()
}

export const navigateToNotebookPage = (notebookTitle: string) =>
  navigateToNotebooksPage().notebookCard(notebookTitle).openNotebookPage()
