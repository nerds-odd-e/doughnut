import { assumeChatAboutNotePage } from "./chatAboutNotePage"

export const assumeNotePage = (noteTopic?: string) => {
  if (noteTopic) {
    cy.findNoteTopic(noteTopic)
  }

  const privateToolbarButton = (btnTextOrTitle: string) => {
    const getButton = () => cy.findByRole("button", { name: btnTextOrTitle })
    return {
      click: () => getButton().click(),
      shouldNotExist: () => getButton().should("not.exist"),
    }
  }

  const clickNotePageMoreOptionsButton = (btnTextOrTitle: string) => {
    privateToolbarButton("more options").click()
    privateToolbarButton(btnTextOrTitle).click()
  }

  return {
    findNoteDetails: (expected: string) => {
      expected.split("\\n").forEach((line) => cy.get("[role=details]").should("contain", line))
    },
    toolbarButton: (btnTextOrTitle: string) => {
      return privateToolbarButton(btnTextOrTitle)
    },
    editNoteButton() {
      return this.toolbarButton("edit note")
    },
    startSearchingAndLinkNote() {
      this.toolbarButton("search and link note").click()
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
      this.toolbarButton("associate wikidata").click()
      cy.replaceFocusedTextAndEnter(wikiID)
    },
    aiSuggestDetailsForNote: () => {
      cy.on("uncaught:exception", () => {
        return false
      })
      cy.findByRole("button", { name: "auto-complete details" }).click()
    },
    chatAboutNote() {
      return assumeChatAboutNotePage()
    },
  }
}
