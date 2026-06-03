import router from '../router'
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
}
