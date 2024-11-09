import NotePath from '../../support/NotePath'
import { notebookCard } from './notebookCard'
import { notebookList } from './NotebookList'
import noteCreationForm from './noteForms/noteCreationForm'
import { assumeNotePage } from './notePage'

const myNotebooksPage = () => {
  cy.findByText('Notebooks')
  return {
    ...notebookList(),
    navigateToPath(notePath: NotePath) {
      return notePath.path.reduce(
        (page, noteTopic) => page.navigateToChild(noteTopic),
        assumeNotePage()
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
