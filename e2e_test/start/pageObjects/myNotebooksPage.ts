/// <reference types="cypress" />
/// <reference path="../../support/index.d.ts" />
import type NotePath from '../../support/NotePath'
import type { assumeNotePage } from './notePage'
import { notebookCard } from './notebookCard'
import { notebookList } from './NotebookList'
import noteCreationForm from './noteForms/noteCreationForm'
import { subscribedNotebooks } from './subscribedNotebooks'

const myNotebooksPage = () => {
  cy.findByText('Notebooks', { selector: 'h2' })

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
    expectNotebookNotToExist(noteTopology: string) {
      cy.get('main').within(() =>
        cy.findCardTitle(noteTopology).should('not.exist')
      )
    },
  }
}

export const routerToMyNotebooksPage = () => {
  cy.pageIsNotLoading()
  cy.routerPush('/d/notebooks', 'notebooks', {})
  return myNotebooksPage()
}
