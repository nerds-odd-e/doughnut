import testability from '../testability'
import { assumeNotePage } from '../pageObjects/notePage'
import { mainMenu } from '../pageObjects/mainMenu'

export const navigationActions = {
  // jumpToNotePage is faster than navigateToPage
  //   it uses the note id memorized when creating them with testability api
  jumpToNotePage(noteTopology: string, forceLoadPage = false) {
    testability()
      .getInjectedNoteIdByTitle(noteTopology)
      .then((noteId) => {
        const url = `/n${noteId}`
        if (forceLoadPage) cy.visit(url)
        else cy.routerPush(url, 'noteShow', { noteId: noteId })
      })

    return assumeNotePage(noteTopology)
  },

  navigateToAssessmentAndCertificatePage() {
    return mainMenu().userOptions().myAssessmentAndCertificateHistory()
  },
}
