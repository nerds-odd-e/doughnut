/// <reference types="cypress" />
/// <reference path="../../support/index.d.ts" />
import type NotePath from '../../support/NotePath'
import type { assumeNotePage } from './notePage'
import { notebookCard } from './notebookCard'
import { notebookList } from './NotebookList'
import noteCreationForm from './noteForms/noteCreationForm'

const myNotebooksPage = () => {
  cy.get('.path-and-content').within(() => {
    cy.findByText('Notebooks')
  })
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
      return noteCreationForm.createNote(notebookTopic)
    },
    notebookCard(notebook: string) {
      return notebookCard(notebook)
    },
  }
}

export const routerToMyNotebooksPage = () => {
  cy.pageIsNotLoading()
  cy.routerPush('/d/notebooks', 'notebooks', {})
  return myNotebooksPage()
}
