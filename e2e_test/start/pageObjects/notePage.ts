import { chatAboutNotePage } from "./chatAboutNotePage"

export const notePage = () => {
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
