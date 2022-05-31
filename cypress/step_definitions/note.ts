/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When, And } from "@badeball/cypress-cucumber-preprocessor"

Given("I visit note {string}", (noteTitle) => {
  cy.jumpToNotePage(noteTitle)
})

Given("there are some notes for the current user", (data) => {
  cy.seedNotes(data.hashes())
})

Given("I have a note with title {string}", (noteTitle: string) => {
  cy.seedNotes([{ title: noteTitle }])
})

Given("there are some notes for existing user {string}", (externalIdentifier, data) => {
  cy.seedNotes(data.hashes(), externalIdentifier)
})

Given("there are notes from Note {int} to Note {int}", (from, to) => {
  const notes = Array(to - from + 1)
    .fill(0)
    .map((_, i) => {
      return { title: `Note ${i + from}` }
    })
  cy.seedNotes(notes)
})

When("I create a notebook with:", (data) => {
  cy.visitMyNotebooks()
  cy.findByText("Add New Notebook").click()
  cy.submitNoteCreationFormsWith(data.hashes())
})

When("I update note {string} to become:", (noteTitle, data) => {
  cy.jumpToNotePage(noteTitle)
  cy.inPlaceEdit(data.hashes()[0])
})

Given("I update note title {string} to become {string}", (noteTitle, newNoteTitle) => {
  cy.jumpToNotePage(noteTitle)
  cy.findByText(noteTitle).click()
  cy.replaceFocusedText(newNoteTitle)
})

Given(
  "I update note {string} description from {string} to become {string}",
  (noteTitle, noteDescription, newNoteDescription) => {
    cy.findByText(noteDescription).click({ force: true })
    cy.replaceFocusedText(newNoteDescription)
  },
)

When("I update note {string} with the description {string}", (noteTitle, newDescription) => {
  cy.jumpToNotePage(noteTitle)
  cy.inPlaceEdit({ Description: newDescription })
})

When("I create note belonging to {string}:", (noteTitle, data) => {
  cy.jumpToNotePage(noteTitle)
  cy.findByText(noteTitle)
  cy.clickAddChildNoteButton()
  cy.submitNoteCreationFormsWith(data.hashes())
})

When("I am creating note under {string}", (noteTitles) => {
  cy.navigateToNotePage(noteTitles)
  cy.clickAddChildNoteButton()
})

Then("I should see {string} in breadcrumb", (noteTitles) => {
  cy.pageIsNotLoading()
  cy.get(".breadcrumb").within(() =>
    noteTitles
      .commonSenseSplit(", ")
      .forEach((noteTitle) => cy.findByText(noteTitle, { timeout: 2000 })),
  )
})

When("I visit all my notebooks", () => {
  cy.visitMyNotebooks()
})

Then("I should see these notes belonging to the user at the top level of all my notes", (data) => {
  cy.visitMyNotebooks()
  cy.expectNoteCards(data.hashes())
})

Then("I should see these notes as children", (data) => {
  cy.expectNoteCards(data.hashes())
})

When("I delete notebook {string}", (noteTitle) => {
  cy.visit("/")
  cy.clickNotePageMoreOptionsButton(noteTitle, "Delete note")
  cy.findByRole("button", { name: "OK" }).click()
})

When("I create a sibling note of {string}:", (noteTitle, data) => {
  cy.findByText(noteTitle)
  cy.findByRole("button", { name: "Add Sibling Note" }).click()
  cy.submitNoteCreationFormsWith(data.hashes())
})

When("I should see that the note creation is not successful", () => {
  cy.findByText("size must be between 1 and 100")
})

Then("I should see {string} in note title", (noteTitle) => {
  cy.expectNoteTitle(noteTitle)
})

Then("I should not see note {string} at the top level of all my notes", (noteTitle) => {
  cy.pageIsNotLoading()
  cy.findByText("Notebooks")
  cy.findByText(noteTitle).should("not.exist")
})

When("I open {string} note from top level", (noteTitles) => {
  cy.navigateToNotePage(noteTitles)
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
  (noteTitle, newDescription) => {
    cy.findByText(noteTitle).click()
    cy.replaceFocusedText(newDescription)
  },
)

When("I should see the screenshot matches", () => {
  // cy.get('.content').compareSnapshot('page-snapshot', 0.001);
})

When("I move note {string} right", (noteTitle) => {
  cy.jumpToNotePage(noteTitle)
  cy.findByText("Move This Note").click()
  cy.findByRole("button", { name: "Move Right" }).click()
})

When(
  "I should see {string} is before {string} in {string}",
  (noteTitle1, noteTitle2, parentNoteTitle) => {
    cy.jumpToNotePage(parentNoteTitle)
    const matcher = new RegExp(noteTitle1 + ".*" + noteTitle2, "g")

    cy.get(".card-title").then(($els) => {
      const texts = Array.from($els, (el) => el.innerText)
      expect(texts).to.match(matcher)
    })
  },
)

// This step definition is for demo purpose
Then("*for demo* I should see there are {int} descendants", (numberOfDescendants) => {
  cy.findByText("" + numberOfDescendants, {
    selector: ".descendant-counter",
  })
})

When("I should be asked to log in again when I click the link {string}", (noteTitle) => {
  cy.on("uncaught:exception", () => {
    return false
  })
  cy.findByText(noteTitle).click()
  cy.get("#username").should("exist")
})

When("I open the {string} view of note {string}", (viewType, noteTitle) => {
  cy.clickNotePageButton(noteTitle, `${viewType} view`, true)
})

When(
  "I should see the note {string} is {int}px * {int}px offset the center of the map",
  (noteTitle, dx, dy) => {
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

When("I click note {string}", (noteTitle) => {
  cy.findByRole("card", { name: noteTitle }).click("bottomRight", {
    force: true,
  })
  //cy.findByRole("card", { name: noteTitle }).click();
})

When("I click note title {string}", (noteTitle) => {
  cy.findByText(noteTitle).click()
})

When("The note {string} {string} have the description indicator", (noteTitle, shouldOrNot) => {
  cy.findByRole("card", { name: noteTitle }).within(() =>
    cy.get(".description-indicator").should(shouldOrNot === "should" ? "exist" : "not.exist"),
  )
})

When("I should see the note {string} is {string}", (noteTitle, highlightedOrNot) => {
  cy.findByRole("card", { name: noteTitle }).should(
    `${highlightedOrNot === "highlighted" ? "" : "not."}have.class`,
    "highlighted",
  )
})

When("I drag the map by {int}px * {int}px", (dx, dy) => {
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

When("I zoom in at the {string}", (position) => {
  cy.get(".mindmap-event-receiver").trigger("wheel", position, {
    clientX: 0,
    clientY: 0,
    deltaY: 50,
  })
})

When("I should see the zoom scale is {string}", (scale) => {
  cy.get(".mindmap-info").findByText(scale)
})

When("I click the zoom indicator", () => {
  cy.get(".mindmap-info").click()
})

When(
  "I should see the notes {string} are around note {string} and apart from each other",
  (noteTitles, parentNoteTitle) => {
    const titles = noteTitles.commonSenseSplit(",")
    cy.findByText(titles[titles.length - 1])
    cy.withinMindmap().then((cards) => {
      titles.forEach((noteTitle) => {
        cy.distanceBetweenCardsGreaterThan(cards, parentNoteTitle, noteTitle, 100)
      })
      cy.distanceBetweenCardsGreaterThan(cards, titles[0], titles[1], 100)
    })
  },
)

Then("I should see the title {string} of the notebook", (noteTitle) => {
  cy.expectNoteTitle(noteTitle)
})

Then("I should see the child notes {string} in order", (notesStr) => {
  const notes = notesStr.split(",")
  notes.forEach((n) => cy.findByText(n))
  cy.findAllByRole("title").then((elms) => {
    let actual = []
    elms.map((i, actualNote) => actual.push(actualNote.innerText))
    actual = actual.filter((c) => notes.includes(c))
    expect(actual.join(",")).to.equal(notes.join(","))
  })
})

Then("I should see {string} is {string} than {string}", (left, aging, right) => {
  let leftColor
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
})

When("I undo {string}", (undoType) => {
  cy.findByTitle(`undo ${undoType}`).click()
})

Then("the deleted notebook with title {string} should be restored", (title) => {
  cy.findByText(title).should("exist")
})

Then("there should be no more undo to do", () => {
  cy.get('.btn[title="undo"]').should("be.disabled")
})

Then("I type {string} in the title", (content) => {
  cy.focused().clear().type(content)
})
