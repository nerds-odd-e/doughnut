import { assumeChatAboutNotePage } from "./chatAboutNotePage"
import submittableForm from "../submittableForm"
import noteCreationForm from "./noteForms/noteCreationForm"

function filterAttributes(attributes: Record<string, string>, keysToKeep: string[]) {
  return Object.keys(attributes)
    .filter((key) => keysToKeep.includes(key))
    .reduce(
      (obj, key) => {
        const val = attributes[key]
        if (val) {
          obj[key] = val
        }
        return obj
      },
      {} as Record<string, string>,
    )
}

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
      clickIfNotOpen: () => {
        getButton().then(($btn) => {
          if ($btn.attr("aria-expanded") === "false") {
            cy.wrap($btn).click()
          }
        })
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
    editNoteImage() {
      return this.toolbarButton("edit note image")
    },
    editAudioButton() {
      return this.toolbarButton("Upload audio")
    },
    downloadAudioFile(fileName: string) {
      const downloadsFolder = Cypress.config("downloadsFolder")
      cy.findByRole("button", { name: `Download ${fileName}` }).click()
      cy.task("fileShouldExistSoon", downloadsFolder + "/" + fileName).should("equal", true)
    },
    updateNoteImage(attributes: Record<string, string>) {
      this.editNoteImage()
        .click()
        .submitWith(filterAttributes(attributes, ["Upload Image", "Image Url", "Use Parent Image"]))
      return this
    },
    updateNoteUrl(attributes: Record<string, string>) {
      this.toolbarButton("edit note url")
        .click()
        .submitWith(filterAttributes(attributes, ["Url"]))
      return this
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
      const openWikidataOptions = () => privateToolbarButton("wikidata options").clickIfNotOpen()

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
