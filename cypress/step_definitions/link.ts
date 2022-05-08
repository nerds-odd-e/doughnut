/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { And, Then, When } from "@badeball/cypress-cucumber-preprocessor"

When("I start searching", () => {
  cy.startSearching()
})

When("I am creating link for note {string}", (noteTitle) => {
  cy.jumpToNotePage(noteTitle)
  cy.startSearching()
})

function makingLink(cy, fromNoteTitle, linkType, toNoteTitle) {
  cy.jumpToNotePage(fromNoteTitle)
  cy.startSearching()
  cy.searchNote(toNoteTitle, ["All My Notebooks And Subscriptions"])
  cy.clickButtonOnCardBody(toNoteTitle, "Select")
  cy.clickRadioByLabel(linkType)
}

When("I link note {string} as {string} note {string}", (fromNoteTitle, linkType, toNoteTitle) => {
  makingLink(cy, fromNoteTitle, linkType, toNoteTitle)
  cy.findByRole("button", { name: "Create Link" }).click()
})

When(
  "I link note {string} as {string} note {string} and move under it",
  (fromNoteTitle, linkType, toNoteTitle) => {
    makingLink(cy, fromNoteTitle, linkType, toNoteTitle)
    cy.getFormControl("Also Move To Under Target Note").check()
    cy.findByRole("button", { name: "Create Link" }).click()
    cy.findByRole("button", { name: "OK" }).click()
  },
)

When(
  "there is {string} link between note {string} and {string}",
  (linkType, fromNoteTitle, toNoteTitle) => {
    cy.createLink(linkType, fromNoteTitle, toNoteTitle)
  },
)

And("I should see the source note as {string}", (noteTitle) => {
  cy.findByText(noteTitle, { selector: "strong" }).should("be.visible")
})

And("I should see {string} as the possible duplicate", (noteTitlesAsString) => {
  cy.tick(500)
  cy.expectExactLinkTargets(noteTitlesAsString.commonSenseSplit(",").map((i) => i.trim()))
})

And(
  "I should see {string} as targets only when searching {string}",
  (noteTitlesAsString, searchKey) => {
    cy.searchNote(searchKey, [])
    cy.expectExactLinkTargets(noteTitlesAsString.commonSenseSplit(",").map((i) => i.trim()))
  },
)

And(
  "I should see {string} as targets only when searching in all my notebooks {string}",
  (noteTitlesAsString, searchKey) => {
    cy.searchNote(searchKey, ["All My Notebooks And Subscriptions"])
    cy.expectExactLinkTargets(noteTitlesAsString.commonSenseSplit(",").map((i) => i.trim()))
  },
)

And(
  "I should see note cannot be found when searching in all my notebooks {string}",
  (searchKey) => {
    cy.searchNote(searchKey, ["All My Notebooks And Subscriptions"])
    cy.findByText("No linkable notes found.").should("be.visible")
  },
)

Then(
  "On the current page, I should see {string} has link {string} {string}",
  (noteTitle, linkType, targetNoteTitles) => {
    cy.findByText(targetNoteTitles.commonSenseSplit(",").pop(), {
      selector: ".link-title",
    })
    cy.findAllByRole("button", { name: linkType })
      .parent()
      .parent()
      .within(() => {
        targetNoteTitles
          .commonSenseSplit(",")
          .forEach((targetNoteTitle) => cy.contains(targetNoteTitle))
      })
  },
)

Then("I should see {string} has no link of type {string}", (noteTitle, linkType) => {
  cy.jumpToNotePage(noteTitle)
  cy.findByText(linkType).should("not.exist")
})

When("I open link {string}", (linkTitle) => {
  cy.findByText(linkTitle).siblings(".link-nob").click()
})

Then("I should be able to change the link to {string}", (linkType) => {
  cy.clickRadioByLabel(linkType)
  cy.pageIsNotLoading()
  cy.findByRole("button", { name: "Update" }).click()
  cy.findAllByRole("button", { name: linkType }).should("be.visible")
})

Then("I should be able to delete the link", () => {
  cy.findByRole("button", { name: "Delete" }).click()
})

Then("I should be able to delete the link to note {string}", (noteTitle) => {
  cy.findByText(noteTitle).siblings(".link-nob").click()
  cy.findByRole("button", { name: "Delete" }).click()
  cy.findByRole("button", { name: "Cancel" }).click()
  cy.findByText(noteTitle).siblings(".link-nob").click()
  cy.findByRole("button", { name: "Delete" }).click()
  cy.findByRole("button", { name: "OK" }).click()
  cy.contains(noteTitle).should("not.exist")
})

Then("I should see the note {string} {string}", (linkType, linkTarget) => {
  cy.log("WHY AM I HERE???")
})
