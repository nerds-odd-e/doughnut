import { assumeChatAboutNotePage } from "./chatAboutNotePage"

export const assumeNotePage = () => {
  const clickNotePageMoreOptionsButton = (btnTextOrTitle: string) => {
    cy.clickNotePageMoreOptionsButtonOnCurrentPage(btnTextOrTitle)
  }

  return {
    startSearchingAndLinkNote() {
      cy.notePageButtonOnCurrentPage("search and link note").click()
    },
    refineNoteDetails() {
      cy.notePageButtonOnCurrentPage("refine").click()
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
      return assumeChatAboutNotePage()
    },
  }
}
