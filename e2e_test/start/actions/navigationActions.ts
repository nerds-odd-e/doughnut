import router from '../router'
import { assumeNotePage } from '../pageObjects/notePage'
import { mainMenu } from '../pageObjects/mainMenu'
import testability from '../testability'

export const navigationActions = {
  jumpToNotePage(noteTopology: string, forceLoadPage = false) {
    testability()
      .getInjectedNoteIdByTitle(noteTopology)
      .then((noteId: number) => {
        const url = `/d/n/${noteId}`
        if (forceLoadPage) cy.visit(url)
        else router().push(url, 'noteShow', { noteId })
      })

    return assumeNotePage(noteTopology)
  },

  navigateToAssessmentAndCertificatePage() {
    return mainMenu().userOptions().myAssessmentAndCertificateHistory()
  },
}
