import { assumeChatAboutNotePage } from "./chatAboutNotePage"
import submittableForm from "../submittableForm"
import noteCreationForm from "./noteForms/noteCreationForm"

export const assumeNotePage = (noteTopic?: string) => {
  if (noteTopic) {
    cy.findByText(noteTopic, { selector: "[role=topic] *" })
  }

  const privateToolbarButton = (btnTextOrTitle: string) => {
    const getButton = () => cy.findByRole("button", { name: btnTextOrTitle })
    return {
      click: () => {
        getButton().click()
        return { ...submittableForm }
      },
      shouldNotExist: () => getButton().should("not.exist"),
    }
  }

  const clickNotePageMoreOptionsButton = (btnTextOrTitle: string) => {
    privateToolbarButton("more options").click()
    privateToolbarButton(btnTextOrTitle).click()
  }

  return {
    navigateToChild: (noteTopic: string) => {
      cy.findCardTitle(noteTopic).click()
      return assumeNotePage(noteTopic)
    },
    findNoteDetails: (expected: string) => {
      expected.split("\\n").forEach((line) => cy.get("[role=details]").should("contain", line))
    },
    toolbarButton: (btnTextOrTitle: string) => {
      return privateToolbarButton(btnTextOrTitle)
    },
    editNoteButton() {
      return this.toolbarButton("edit note")
    },
    editAudioButton() {
      return this.toolbarButton("Upload audio")
    },
    audioFileDownloadButton(fileName: string) {
      const getButton = () => cy.findByRole("button", { name: `Download ${fileName}` })
      return {
        click: () => {
          getButton().click()
        },
        shouldNotExist: () => getButton().should("not.exist"),
      }
    },
    downloadAudioFile(fileName: string) {
      this.audioFileDownloadButton(fileName).click()
      const downloadsFolder = Cypress.config("downloadsFolder")
      cy.task("fileShouldExistSoon", downloadsFolder + "/" + fileName).should("equal", true)
    },
    updateNoteAccessories(attributes: Record<string, string>) {
      this.editNoteButton().click().submitWith(attributes)
    },
    startSearchingAndLinkNote() {
      this.toolbarButton("search and link note").click()
    },
    addingChildNote() {
      cy.pageIsNotLoading()
      this.toolbarButton("Add Child Note").click()
      return noteCreationForm
    },
    addingSiblingNote() {
      cy.pageIsNotLoading()
      this.toolbarButton("Add Sibling Note").click()
      return noteCreationForm
    },
    aiGenerateImage() {
      clickNotePageMoreOptionsButton("Generate Image with DALL-E")
    },
    deleteNote() {
      clickNotePageMoreOptionsButton("Delete note")
      cy.findByRole("button", { name: "OK" }).click()
      cy.pageIsNotLoading()
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
    wikidataOptions() {
      const openWikidataOptions = () => privateToolbarButton("wikidata options").click()

      return {
        associate(wikiID: string) {
          privateToolbarButton("associate wikidata").click()
          cy.replaceFocusedTextAndEnter(wikiID)
        },
        reassociationWith(wikiID: string) {
          openWikidataOptions()
          privateToolbarButton("Edit Wikidata Id").click()
          cy.replaceFocusedTextAndEnter(wikiID)
        },
        hasAssociation() {
          openWikidataOptions()
          const elm = () => {
            return cy.findByRole("button", { name: "Go to Wikidata" })
          }
          elm()

          return {
            expectALinkThatOpensANewWindowWithURL(url: string) {
              cy.window().then((win) => {
                const popupWindowStub = { location: { href: undefined }, focus: cy.stub() }
                cy.stub(win, "open").as("open").returns(popupWindowStub)
                elm().click()
                cy.get("@open").should("have.been.calledWith", "")
                // using a callback so that cypress can wait until the stubbed value is assigned
                cy.wrap(() => popupWindowStub.location.href)
                  .should((cb) => expect(cb()).equal(url))
                  .then(() => {
                    expect(popupWindowStub.focus).to.have.been.called
                  })
              })
            },
          }
        },
      }
    },
  }
}
