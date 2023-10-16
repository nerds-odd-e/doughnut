// jumptoNotePage is faster than navigateToNotePage

import { chatAboutNotePage } from "./chatAboutNotePage"

//    it uses the note id memorized when creating them with testability api
export const jumpToNotePage = (noteTopic: string, forceLoadPage = false) => {
  cy.testability()
    .getSeededNoteIdByTitle(noteTopic)
    .then((noteId) => {
      const url = `/notes/${noteId}`
      if (forceLoadPage) cy.visit(url)
      else cy.routerPush(url, "noteShow", { noteId: noteId })
    })
  cy.findNoteTopic(noteTopic)

  const clickNotePageMoreOptionsButton = (btnTextOrTitle: string) => {
    cy.clickNotePageMoreOptionsButtonOnCurrentPage(btnTextOrTitle)
  }

  return {
    startSearchingAndLinkNote() {
      cy.notePageButtonOnCurrentPage("search and link note").click()
    },
    aiGenerateImage() {
      clickNotePageMoreOptionsButton("Generate Image with DALL-E")
    },
    deleteNote() {
      clickNotePageMoreOptionsButton("Delete note")
      cy.findByRole("button", { name: "OK" }).click()
      cy.pageIsNotLoading()
    },
    associateNoteWithWikidataId(wikiID: string) {
      cy.notePageButtonOnCurrentPage("associate wikidata").click()
      cy.replaceFocusedTextAndEnter(wikiID)
    },
    aiSuggestDetailsForNote: () => {
      cy.on("uncaught:exception", () => {
        return false
      })
      cy.findByRole("button", { name: "suggest details" }).click()
    },
    chatAboutNote() {
      clickNotePageMoreOptionsButton("chat about this note")
      return chatAboutNotePage()
    },
  }
}
