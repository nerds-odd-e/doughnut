import NotePath from '../../support/NotePath'
import { notebookCard } from './notebookCard'
import noteCreationForm from './noteForms/noteCreationForm'
import { assumeNotePage } from './notePage'

const notebooksPage = () => {
  cy.findByText('Notebooks')
  return {
    navigateToPath(notePath: NotePath) {
      return notePath.path.reduce(
        (page, noteTopic) => page.navigateToChild(noteTopic),
        assumeNotePage()
      )
    },
    creatingNotebook(notebookTopic: string) {
      cy.findByText('Add New Notebook').click()
      return noteCreationForm.createNote(notebookTopic, undefined)
    },
    notebookCard(notebook: string) {
      return notebookCard(notebook)
    },
  }
}

export const routerToNotebooksPage = () => {
  cy.pageIsNotLoading()
  cy.routerPush('/d/notebooks', 'notebooks', {})
  return notebooksPage()
}
