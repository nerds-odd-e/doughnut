import router from '../router'
import { assumeNotePage } from '../pageObjects/notePage'
import { mainMenu } from '../pageObjects/mainMenu'
import { wikiBasenameFromTitle } from '../wikiSlug'

export const navigationActions = {
  jumpToNotePage(noteTopology: string, forceLoadPage = false) {
    const basename = wikiBasenameFromTitle(noteTopology)
    const url = `/d/notes/${encodeURIComponent(basename)}`
    if (forceLoadPage) cy.visit(url)
    else router().push(url, 'noteShow', { noteId: basename })

    return assumeNotePage(noteTopology)
  },

  jumpToNotePageById(noteId: number) {
    const url = `/n${noteId}`
    router().push(url, 'noteShow', { noteId })
    return assumeNotePage()
  },

  navigateToAssessmentAndCertificatePage() {
    return mainMenu().userOptions().myAssessmentAndCertificateHistory()
  },
}
