import router from '../router'
import { pageIsNotLoading } from '../pageBase'
import notebookPage from '../pageObjects/notebookPage'
import { assumeNotePage } from '../pageObjects/notePage'
import testability from '../testability'

export const navigationActions = {
  jumpToNotePage(noteTopology: string, forceLoadPage = false) {
    testability()
      .getInjectedNoteIdByTitle(noteTopology)
      .then((noteId: number) => {
        const url = `/n${noteId}`
        if (forceLoadPage) cy.visit(url)
        else router().push(url, 'noteShow', { noteId })
      })

    return assumeNotePage(noteTopology)
  },

  jumpToNotebookPage(notebookName: string) {
    testability()
      .getNotebookIdByName(notebookName)
      .then((notebookId: number) => {
        router().push(`/notebooks/${notebookId}`, 'notebookPage', {
          notebookId,
        })
      })
    pageIsNotLoading()
    return notebookPage()
  },
}
