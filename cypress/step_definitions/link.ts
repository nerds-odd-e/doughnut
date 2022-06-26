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
    cy.testability().seedLink(linkType, fromNoteTitle, toNoteTitle)
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
    cy.findByText("No matching notes found.").should("be.visible")
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

Then("I should see {string} has no link to {string}", (noteTitle: string, targetTitle: string) => {
  cy.jumpToNotePage(noteTitle)
  cy.findByText(targetTitle).should("not.exist")
})

Then(
  "I change the link from {string} to {string} to {string}",
  (noteTitle: string, targetTitle: string, linkType: string) => {
    cy.jumpToNotePage(noteTitle)
    cy.clickLinkNob(targetTitle)
    cy.clickRadioByLabel(linkType)
    cy.pageIsNotLoading()
    cy.findByRole("button", { name: "Update" }).click()
    cy.findAllByRole("button", { name: linkType }).should("be.visible")
  },
)

Then("I should be able to delete the link", () => {
  cy.findByRole("button", { name: "Delete" }).click()
})

Then("I delete the link from {string} to {string}", (noteTitle, targetTitle) => {
  cy.pageIsNotLoading()
  cy.jumpToNotePage(noteTitle)
  cy.clickLinkNob(targetTitle)
  cy.findByRole("button", { name: "Delete" }).click()
  cy.findByRole("button", { name: "Cancel" }).click()
  cy.clickLinkNob(targetTitle)
  cy.findByRole("button", { name: "Delete" }).click()
  cy.findByRole("button", { name: "OK" }).click()
  cy.contains(targetTitle).should("not.exist")
  cy.findByText(noteTitle) // remain on the same note page
})
