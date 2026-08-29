import router from '../router'
import { waitUntilAppIsNotBusy } from '../pageBase'
import bookReadingPage from '../pageObjects/bookReadingPage'
import notebookPage from '../pageObjects/notebookPage'
import { assumeNotePage } from '../pageObjects/notePage'
import testability from '../testability'

export const navigationActions = {
  jumpToNotePage(noteTopology: string, forceLoadPage = false) {
    testability()
      .getInjectedNoteIdByTitle(noteTopology)
      .then((noteId: number) => {
        if (forceLoadPage) {
          router().visitNamed('noteShow', { noteId })
        } else {
          router().push('noteShow', { noteId })
        }
      })

    return assumeNotePage(noteTopology)
  },

  jumpToNoteProperty(noteTopology: string, propertyKey: string) {
    testability()
      .getInjectedNoteIdByTitle(noteTopology)
      .then((noteId: number) => {
        router().push('noteProperty', { noteId, propertyKey })
      })
    waitUntilAppIsNotBusy()
    return assumeNotePage(noteTopology)
  },

  jumpToNotebookPage(notebookName: string) {
    testability()
      .getNotebookIdByName(notebookName)
      .then((notebookId: number) => {
        router().push('notebookPage', { notebookId })
      })
    waitUntilAppIsNotBusy()
    return notebookPage()
  },

  jumpToBookReadingPage(notebookName: string) {
    return testability()
      .getNotebookIdByName(notebookName)
      .then((notebookId: number) => {
        router().push('bookReading', { notebookId })
      })
      .then(() => bookReadingPage())
  },

  jumpToFolderPage(folderLabel: string, notebookName: string) {
    testability()
      .getNotebookIdByName(notebookName)
      .then((notebookId: number) =>
        testability()
          .getFolderIdInNotebook(notebookId, folderLabel)
          .then((folderId: number) => {
            router().push('folderPage', { notebookId, folderId })
          })
      )
    waitUntilAppIsNotBusy()
  },
}
