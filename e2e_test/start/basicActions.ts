import { assumeChatAboutNotePage } from "./pageObjects/chatAboutNotePage"
import { assumeNotePage } from "./pageObjects/notePage"

export default {
  // jumptoNotePage is faster than navigateToNotePage
  //    it uses the note id memorized when creating them with testability api
  jumpToNotePage: (noteTopic: string, forceLoadPage = false) => {
    cy.testability()
      .getSeededNoteIdByTitle(noteTopic)
      .then((noteId) => {
        const url = `/notes/${noteId}`
        if (forceLoadPage) cy.visit(url)
        else cy.routerPush(url, "noteShow", { noteId: noteId })
      })
    cy.findNoteTopic(noteTopic)

    return assumeNotePage()
  },
  assumeChatAboutNotePage,
}
