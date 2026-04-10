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

const myNotebooksPage = () => {
  addNewNotebookButton()

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
    creatingNotebookGroup(name: string) {
      cy.findByRole('button', { name: 'New notebook group' }).click()
      cy.findByLabelText('Group name').type(name)
      cy.findByRole('button', { name: 'Create notebook group' }).click()
      pageIsNotLoading()
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
