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

defineParameterType({
  name: "notepath",
  regexp: /.*/,
  transformer(s: string) {
    return new NotePath(s)
  },
})

Given("I visit note {string}", (noteTitle) => {
  cy.jumpToNotePage(noteTitle)
})

Given("there are some notes for the current user:", (data: DataTable) => {
  cy.testability().seedNotes(data.hashes())
})

Given("I have a note with the title {string}", (noteTitle: string) => {
  cy.testability().seedNotes([{ title: noteTitle }])
})

Given("there are some notes for existing user {string}", (externalIdentifier, data: DataTable) => {
  cy.testability().seedNotes(data.hashes(), externalIdentifier)
})

Given("there are notes from Note {int} to Note {int}", (from: number, to: number) => {
  const notes = Array(to - from + 1)
    .fill(0)
    .map((_, i) => {
      return { title: `Note ${i + from}` }
    })
  cy.testability().seedNotes(notes)
})

When("I create notebooks with:", (notes: DataTable) => {
  notes.hashes().forEach((noteAttributes) => {
    cy.createNotebookWith(noteAttributes)
    cy.dialogDisappeared()
  })
})

When("I create a notebook with empty title", () => {
  cy.createNotebookWith({ Title: "" })
})

When("I update note {string} to become:", (noteTitle: string, data: DataTable) => {
  cy.jumpToNotePage(noteTitle)
  cy.inPlaceEdit(data.hashes()[0])
})

When("I update note accessories of {string} to become:", (noteTitle: string, data: DataTable) => {
  cy.jumpToNotePage(noteTitle)
  cy.openAndSubmitNoteAccessoriesFormWith(noteTitle, data.hashes()[0])
})

When(
  "I should see note {string} has a picture and a url {string}",
  (noteTitle: string, expectedUrl: string) => {
    cy.jumpToNotePage(noteTitle)
    cy.get("#note-picture").should("exist")
    cy.findByLabelText("Url:").should("have.attr", "href", expectedUrl)
  },
)

When("I can change the title {string} to {string}", (noteTitle: string, newNoteTitle: string) => {
  cy.findNoteTitle(noteTitle)
  cy.inPlaceEdit({ title: newNoteTitle })
  cy.findNoteTitle(newNoteTitle)
})

Given(
  "I update note title {string} to become {string}",
  (noteTitle: string, newNoteTitle: string) => {
    cy.jumpToNotePage(noteTitle)
    cy.findNoteTitle(noteTitle).click()
    cy.replaceFocusedTextAndEnter(newNoteTitle)
  },
)

Given(
  "I update note {string} description from {string} to become {string}",
  (noteTitle: string, noteDescription: string, newNoteDescription: string) => {
    cy.findByText(noteDescription).click({ force: true })
    cy.replaceFocusedTextAndEnter(newNoteDescription)
  },
)

When(
  "I update note {string} with description {string}",
  (noteTitle: string, newDescription: string) => {
    cy.jumpToNotePage(noteTitle)
    cy.inPlaceEdit({ Description: newDescription })
    cy.findNoteDescriptionOnCurrentPage(newDescription)
  },
)

When("I create a note belonging to {string}:", (noteTitle: string, data: DataTable) => {
  expect(data.hashes().length).to.equal(1)
  cy.jumpToNotePage(noteTitle)
  cy.clickAddChildNoteButton()
  cy.submitNoteCreationFormSuccessfully(data.hashes()[0])
})

When("I try to create a note belonging to {string}:", (noteTitle: string, data: DataTable) => {
  expect(data.hashes().length).to.equal(1)
  cy.jumpToNotePage(noteTitle)
  cy.clickAddChildNoteButton()
  cy.submitNoteCreationFormWith(data.hashes()[0])
})

When("I am creating a note under {notepath}", (notePath: NotePath) => {
  cy.navigateToNotePage(notePath)
  cy.clickAddChildNoteButton()
})

Then("I should see {string} in breadcrumb", (noteTitles: string) => {
  cy.pageIsNotLoading()
  cy.expectBreadcrumb(noteTitles, false)
})

Then("I should see {string} in breadcrumb with add sibling button", (noteTitles: string) => {
  cy.pageIsNotLoading()
  cy.expectBreadcrumb(noteTitles, true)
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

When("I delete notebook {string}", (noteTitle: string) => {
  cy.deleteNote(noteTitle)
})

When("I delete note {string} at {int}:00", (noteTitle: string, hour: number) => {
  cy.testability().backendTimeTravelTo(0, hour)
  cy.deleteNote(noteTitle)
})

When("I delete note {string}", (noteTitle: string) => {
  cy.deleteNote(noteTitle)
})

When("I create a sibling note of {string}:", (noteTitle: string, data: DataTable) => {
  expect(data.hashes().length).to.equal(1)
  cy.findNoteTitle(noteTitle)
  cy.addSiblingNoteButton().click()
  cy.submitNoteCreationFormSuccessfully(data.hashes()[0])
})

When("I should see that the note creation is not successful", () => {
  cy.expectFieldErrorMessage("Title", "size must be between 1 and 100")
  cy.dismissLastErrorMessage()
})

Then("I should see the note {string} is marked as deleted", (noteTitle: string) => {
  cy.jumpToNotePage(noteTitle)
  cy.findNoteTitle(noteTitle)
  cy.findByText("This note has been deleted")
})

Then("I should not see note {string} at the top level of all my notes", (noteTitle: string) => {
  cy.pageIsNotLoading()
  cy.findByText("Notebooks")
  cy.findCardTitle(noteTitle).should("not.exist")
})

When("I navigate to {notepath} note", (notePath: NotePath) => {
  cy.navigateToNotePage(notePath)
})

When("I click the child note {string}", (noteTitle) => {
  cy.navigateToChild(noteTitle)
})

When("I move note {string} left", (noteTitle) => {
  cy.jumpToNotePage(noteTitle)
  cy.findByText("Move This Note").click()
  cy.findByRole("button", { name: "Move Left" }).click()
})

When(
  "I double click {string} and edit the description to {string}",
  (oldDescription: string, newDescription: string) => {
    cy.findByText(oldDescription).click()
    cy.replaceFocusedTextAndEnter(newDescription)
  },
)

When("I should see the screenshot matches", () => {
  // cy.get('.content').compareSnapshot('page-snapshot', 0.001);
})

When("I move note {string} right", (noteTitle: string) => {
  cy.jumpToNotePage(noteTitle)
  cy.findByText("Move This Note").click()
  cy.findByRole("button", { name: "Move Right" }).click()
})

When(
  "I should see {string} is before {string} in {string}",
  (noteTitle1: string, noteTitle2: string, parentNoteTitle: string) => {
    cy.jumpToNotePage(parentNoteTitle)
    const matcher = new RegExp(noteTitle1 + ".*" + noteTitle2, "g")

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

When("I should be asked to log in again when I click the link {string}", (noteTitle: string) => {
  cy.on("uncaught:exception", () => {
    return false
  })
  cy.findCardTitle(noteTitle).click()
  cy.get("#username").should("exist")
})

When("I open the {string} view of note {string}", (viewType: string, noteTitle: string) => {
  cy.selectViewOfNote(noteTitle, viewType)
})

When(
  "I should see the note {string} is {int}px * {int}px offset the center of the map",
  (noteTitle: string, dx: number, dy: number) => {
    cy.withinMindmap().then((cards) => {
      const rect = cards.mindmapRect
      const frameCenterX = rect.left + rect.width / 2
      const frameCenterY = rect.top + rect.height / 2
      const cardRect = cards[noteTitle]
      const cardCenterX = cardRect.left + cardRect.width / 2
      const cardCenterY = cardRect.top + cardRect.height / 2
      expect(cardCenterX - frameCenterX).to.closeTo(dx, 10)
      expect(cardCenterY - frameCenterY).to.closeTo(dy, 30)
    })
  },
)

When("I click note {string} avoiding the title", (noteTitle: string) => {
  cy.findByRole("card", { name: noteTitle }).click("bottomRight", {
    force: true,
  })
})

When(
  "The note {string} {string} have the description indicator",
  (noteTitle: string, shouldOrNot: string) => {
    cy.findByRole("card", { name: noteTitle }).within(() =>
      cy.get(".description-indicator").should(shouldOrNot === "should" ? "exist" : "not.exist"),
    )
  },
)

When(
  "I should see the note {string} is {string}",
  (noteTitle: string, highlightedOrNot: string) => {
    cy.findByRole("card", { name: noteTitle }).should(
      `${highlightedOrNot === "highlighted" ? "" : "not."}have.class`,
      "highlighted",
    )
  },
)

When("I drag the map by {int}px * {int}px", (dx: number, dy: number) => {
  cy.get(".mindmap-event-receiver")
    .trigger("pointerdown", "topLeft")
    .trigger("pointermove", "topLeft", { clientX: dx, clientY: dy })
    .trigger("pointerup", { force: true })
})

When("I drag the map by {int}px * {int}px when holding the shift button", (dx, dy) => {
  cy.get(".mindmap-event-receiver")
    .trigger("pointerdown", "topLeft")
    .trigger("pointermove", "topLeft", {
      shiftKey: true,
      clientX: dx,
      clientY: dy,
    })
    .trigger("pointerup", { force: true })
})

When("I zoom in at the {string}", (position: string) => {
  cy.get(".mindmap-event-receiver").trigger("wheel", Number(position), {
    clientX: 0,
    clientY: 0,
    deltaY: 50,
  })
})

When("I should see the zoom scale is {string}", (scale: string) => {
  cy.get(".mindmap-info").findByText(scale)
})

When("I click the zoom indicator", () => {
  cy.get(".mindmap-info").click()
})

When(
  "I should see the notes {string} are around note {string} and apart from each other",
  (noteTitles: string, parentNoteTitle: string) => {
    const titles = noteTitles.commonSenseSplit(",")
    cy.findMindmapCardTitle(titles[titles.length - 1])
    cy.withinMindmap().then((cards) => {
      titles.forEach((noteTitle: string) => {
        cy.distanceBetweenCardsGreaterThan(cards, parentNoteTitle, noteTitle, 100)
      })
      cy.distanceBetweenCardsGreaterThan(cards, titles[0], titles[1], 100)
    })
  },
)

Then("I should see the title {string} of the notebook", (noteTitle: string) => {
  cy.findNoteTitle(noteTitle)
})

Then("I should see the child notes {string} in order", (notesStr: string) => {
  const notes = notesStr.split(",")
  notes.forEach((n) => cy.findByText(n))
  cy.findAllByRole("title").then((elms) => {
    let actual: string[] = []
    elms.map((i, actualNote) => actual.push(actualNote.innerText))
    actual = actual.filter((c) => notes.includes(c))
    expect(actual.join(",")).to.equal(notes.join(","))
  })
})

Then(
  "I should see {string} is {string} than {string}",
  (left: string, aging: string, right: string) => {
    let leftColor: string
    cy.pageIsNotLoading()
    cy.jumpToNotePage(left)
    cy.get(".note-body")
      .invoke("css", "border-color")
      .then((val) => (leftColor = val))
    cy.jumpToNotePage(right)
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

Then("the deleted notebook with title {string} should be restored", (title: string) => {
  cy.findNoteTitle(title)
})

Then("there should be no more undo to do", () => {
  cy.get('.btn[title="undo"]').should("not.exist")
})

Then("I type {string} in the title", (content: string) => {
  cy.focused().clear().type(content)
})

Then(
  "I should see the note description on current page becomes {string}",
  (descriptionText: string) => {
    cy.findNoteDescriptionOnCurrentPage(descriptionText)
  },
)

When("I generate an image for {string}", (noteTitle: string) => {
  cy.aiGenerateImage(noteTitle)
})

Then("I should find an art created by the ai", () => {
  cy.get("img.ai-art").should("be.visible")
})

Given("I ask to complete the description for note {string}", (noteTitle: string) => {
  cy.aiSuggestDescriptionForNote(noteTitle)
})

Then("I should see that the open AI service is not available in controller bar", () => {
  cy.get(".last-error-message")
    .should((elem) => {
      expect(elem.text()).to.equal("The OpenAI request was not Authorized.")
    })
    .click()
})

Then(
  "I change the instruction of note {string} to {string}",
  (noteTitle: string, instruction: string) => {
    cy.jumpToNotePage(noteTitle)
    cy.openAndSubmitNoteAccessoriesFormWith(noteTitle, {
      "Question Generation Instruction": instruction,
    })
  },
)
