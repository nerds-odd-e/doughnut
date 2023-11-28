/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import {
  DataTable,
  defineParameterType,
  Given,
  Then,
  When,
} from "@badeball/cypress-cucumber-preprocessor"
import NotePath from "../support/NotePath"
import "../support/string.extensions"
import start from "../start/index"

defineParameterType({
  name: "notepath",
  regexp: /.*/,
  transformer(s: string) {
    return new NotePath(s)
  },
})

Given("I visit note {string}", (noteTopic) => {
  start.jumpToNotePage(noteTopic)
})

Given("there are some notes for the current user:", (data: DataTable) => {
  start.testability().seedNotes(data.hashes())
})

Given("I have a note with the topic {string}", (noteTopic: string) => {
  start.testability().seedNotes([{ topic: noteTopic }])
})

Given("there are some notes for existing user {string}", (externalIdentifier, data: DataTable) => {
  start.testability().seedNotes(data.hashes(), externalIdentifier)
})

Given("there are notes from Note {int} to Note {int}", (from: number, to: number) => {
  const notes = Array(to - from + 1)
    .fill(0)
    .map((_, i) => {
      return { topic: `Note ${i + from}` }
    })
  start.testability().seedNotes(notes)
})

When("I create notebooks with:", (notes: DataTable) => {
  notes.hashes().forEach((noteAttributes) => {
    cy.createNotebookWith(noteAttributes)
    cy.dialogDisappeared()
  })
})

When("I create a notebook with empty topic", () => {
  cy.createNotebookWith({ Topic: "" })
})

When("I update note {string} to become:", (noteTopic: string, data: DataTable) => {
  start.jumpToNotePage(noteTopic)
  cy.inPlaceEdit(data.hashes()[0])
})

When("I update note accessories of {string} to become:", (noteTopic: string, data: DataTable) => {
  start.jumpToNotePage(noteTopic)
  cy.openAndSubmitNoteAccessoriesFormWith(noteTopic, data.hashes()[0])
})

When(
  "I should see note {string} has a picture and a url {string}",
  (noteTopic: string, expectedUrl: string) => {
    start.jumpToNotePage(noteTopic)
    cy.get("#note-picture").should("exist")
    cy.findByLabelText("Url:").should("have.attr", "href", expectedUrl)
  },
)

When("I can change the topic {string} to {string}", (noteTopic: string, newNoteTopic: string) => {
  cy.findNoteTopic(noteTopic)
  cy.inPlaceEdit({ topic: newNoteTopic })
  cy.findNoteTopic(newNoteTopic)
})

Given(
  "I update note topic {string} to become {string}",
  (noteTopic: string, newNoteTopic: string) => {
    start.jumpToNotePage(noteTopic)
    cy.findNoteTopic(noteTopic).click()
    cy.replaceFocusedTextAndEnter(newNoteTopic)
  },
)

Given(
  "I update note {string} details from {string} to become {string}",
  (noteTopic: string, noteDetails: string, newNoteDetails: string) => {
    cy.findByText(noteDetails).click({ force: true })
    cy.replaceFocusedTextAndEnter(newNoteDetails)
  },
)

When("I update note {string} with details {string}", (noteTopic: string, newDetails: string) => {
  start.jumpToNotePage(noteTopic)
  cy.inPlaceEdit({ Details: newDetails })
  cy.findNoteDetailsOnCurrentPage(newDetails)
})

When("I create a note belonging to {string}:", (noteTopic: string, data: DataTable) => {
  expect(data.hashes().length).to.equal(1)
  start.jumpToNotePage(noteTopic)
  cy.clickAddChildNoteButton()
  cy.submitNoteCreationFormSuccessfully(data.hashes()[0])
})

When("I try to create a note belonging to {string}:", (noteTopic: string, data: DataTable) => {
  expect(data.hashes().length).to.equal(1)
  start.jumpToNotePage(noteTopic)
  cy.clickAddChildNoteButton()
  cy.submitNoteCreationFormWith(data.hashes()[0])
})

When("I am creating a note under {notepath}", (notePath: NotePath) => {
  cy.navigateToNotePage(notePath)
  cy.clickAddChildNoteButton()
})

Then("I should see {string} in breadcrumb", (noteTopics: string) => {
  cy.pageIsNotLoading()
  cy.expectBreadcrumb(noteTopics)
})

When("I visit all my notebooks", () => {
  cy.routerToNotebooks()
})

Then(
  "I should see these notes belonging to the user at the top level of all my notes",
  (data: DataTable) => {
    cy.routerToNotebooks()
    cy.expectNoteCards(data.hashes())
  },
)

Then("I should see {notepath} with these children", (notePath: NotePath, data: DataTable) => {
  cy.navigateToNotePage(notePath).then(() => cy.expectNoteCards(data.hashes()))
})

When("I delete notebook {string}", (noteTopic: string) => {
  start.jumpToNotePage(noteTopic).deleteNote()
})

When("I delete note {string} at {int}:00", (noteTopic: string, hour: number) => {
  start.testability().backendTimeTravelTo(0, hour)
  start.jumpToNotePage(noteTopic).deleteNote()
})

When("I delete note {string}", (noteTopic: string) => {
  start.jumpToNotePage(noteTopic).deleteNote()
})

When("I create a sibling note of {string}:", (noteTopic: string, data: DataTable) => {
  expect(data.hashes().length).to.equal(1)
  cy.findNoteTopic(noteTopic)
  cy.addSiblingNoteButton().click()
  cy.submitNoteCreationFormSuccessfully(data.hashes()[0])
})

When("I should see that the note creation is not successful", () => {
  cy.expectFieldErrorMessage("Topic", "size must be between 1 and 150")
  cy.dismissLastErrorMessage()
})

Then("I should see the note {string} is marked as deleted", (noteTopic: string) => {
  start.jumpToNotePage(noteTopic)
  cy.findNoteTopic(noteTopic)
  cy.findByText("This note has been deleted")
})

Then("I should not see note {string} at the top level of all my notes", (noteTopic: string) => {
  cy.pageIsNotLoading()
  cy.findByText("Notebooks")
  cy.findCardTitle(noteTopic).should("not.exist")
})

When("I navigate to {notepath} note", (notePath: NotePath) => {
  cy.navigateToNotePage(notePath)
})

When("I click the child note {string}", (noteTopic) => {
  cy.navigateToChild(noteTopic)
})

When("I move note {string} left", (noteTopic) => {
  start.jumpToNotePage(noteTopic)
  cy.findByText("Move This Note").click()
  cy.findByRole("button", { name: "Move Left" }).click()
})

When("I should see the screenshot matches", () => {
  // cy.get('.content').compareSnapshot('page-snapshot', 0.001);
})

When("I move note {string} right", (noteTopic: string) => {
  start.jumpToNotePage(noteTopic)
  cy.findByText("Move This Note").click()
  cy.findByRole("button", { name: "Move Right" }).click()
})

When(
  "I should see {string} is before {string} in {string}",
  (noteTopic1: string, noteTopic2: string, parentNoteTopic: string) => {
    start.jumpToNotePage(parentNoteTopic)
    const matcher = new RegExp(noteTopic1 + ".*" + noteTopic2, "g")

    cy.get(".card-title").then(($els) => {
      const texts = Array.from($els, (el) => el.innerText)
      expect(texts).to.match(matcher)
    })
  },
)

// This step definition is for demo purpose
Then("*for demo* I should see there are {int} descendants", (numberOfDescendants: number) => {
  cy.findByText("" + numberOfDescendants, {
    selector: ".descendant-counter",
  })
})

When("I should be asked to log in again when I click the link {string}", (noteTopic: string) => {
  cy.on("uncaught:exception", () => {
    return false
  })
  cy.findCardTitle(noteTopic).click()
  cy.get("#username").should("exist")
})

Then(
  "I should see {string} is {string} than {string}",
  (left: string, aging: string, right: string) => {
    let leftColor: string
    cy.pageIsNotLoading()
    start.jumpToNotePage(left)
    cy.get(".note-body")
      .invoke("css", "border-color")
      .then((val) => (leftColor = val))
    start.jumpToNotePage(right)
    cy.get(".note-body")
      .invoke("css", "border-color")
      .then((val) => {
        const leftColorIndex = parseInt(leftColor.match(/\d+/)[0])
        const rightColorIndex = parseInt(val.match(/\d+/)[0])
        if (aging === "newer") {
          expect(leftColorIndex).to.greaterThan(rightColorIndex)
        } else {
          expect(leftColorIndex).to.equal(rightColorIndex)
        }
      })
  },
)

When("I undo {string}", (undoType: string) => {
  cy.undoLast(undoType)
})

When("I undo {string} again", (undoType: string) => {
  cy.undoLast(undoType)
})

Then("the deleted notebook with topic {string} should be restored", (topic: string) => {
  cy.findNoteTopic(topic)
})

Then("there should be no more undo to do", () => {
  cy.get('.btn[title="undo"]').should("not.exist")
})

Then("I type {string} in the topic", (content: string) => {
  cy.focused().clear().type(content)
})

Then("the note details on the current page should be {string}", (detailsText: string) => {
  cy.findNoteDetailsOnCurrentPage(detailsText)
})

When("I accept the AI suggestion", () => {
  cy.findNoteDetailsOnCurrentPage("").type("{enter}")
})

When("I generate an image for {string}", (noteTopic: string) => {
  start.jumpToNotePage(noteTopic).aiGenerateImage()
})

Then("I should find an art created by the ai", () => {
  cy.get("img.ai-art").should("be.visible")
})

Given("I request to complete the details for the note {string}", (noteTopic: string) => {
  start.jumpToNotePage(noteTopic).aiSuggestDetailsForNote()
})

Then("I should see a notification of OpenAI service unavailability in the controller bar", () => {
  cy.get(".last-error-message")
    .should((elem) => {
      expect(elem.text()).to.equal("The OpenAI request was not Authorized.")
    })
    .click()
})

When("I start to chat about the note {string}", (noteTopic: string) => {
  start.jumpToNotePage(noteTopic).chatAboutNote()
})

When("I click the Refine button", (noteTopic: string) => {
  start.jumpToNotePage(noteTopic).refineNoteDetails()
})

When("I answer with {string} to the clarifying question {string}", (answer: string) => {
  cy.findNoteDetailsCompletionDialogInput().type(answer)
  cy.sendCompletionAnswer()
})

When('I respond with "cancel" to the clarifying question {string}', () => {
  start.assumeDialogIsOpen().close()
})

When("I type in the details the word {string} followed by a space", (detailsText: string) => {
  cy.findNoteDetailsOnCurrentPage("").type(detailsText)
})

Then("the note details are {string}", (detailsText: string) => {
  cy.findNoteDetailsOnCurrentPage(detailsText)
})
When("I continue typing {string}", (additionalText: string) => {
  cy.focused().type(additionalText)
})
