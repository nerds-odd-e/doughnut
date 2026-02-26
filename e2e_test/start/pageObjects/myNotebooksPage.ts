/// <reference types="cypress" />
import { pageIsNotLoading } from '../pageBase'
import router from '../router'
import type NotePath from '../../support/NotePath'
import type { assumeNotePage } from './notePage'
import { notebookCard } from './notebookCard'
import { notebookList } from './NotebookList'
import noteCreationForm from './noteForms/noteCreationForm'
import { subscribedNotebooks } from './subscribedNotebooks'

const myNotebooksPage = () => {
  cy.findByText('Add New Notebook')

  return {
    ...notebookList(),
    navigateToPath(notePath: NotePath) {
      return notePath.path.reduce<ReturnType<typeof assumeNotePage>>(
        (page, noteTopology) => page.navigateToChild(noteTopology),
        this as any
      )
    },
    creatingNotebook(notebookTopic: string) {
      cy.findByText('Add New Notebook').click()
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
  }
}

export const navigateToNotebooksPage = () => {
  pageIsNotLoading()
  router().push('/d/notebooks', 'notebooks', {})
  return myNotebooksPage()
}
