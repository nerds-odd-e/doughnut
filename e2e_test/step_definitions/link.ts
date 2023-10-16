/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from "@badeball/cypress-cucumber-preprocessor"
import "../support/string.extensions"
import start from "start"

When("I start searching", () => {
  cy.startSearching()
})

When("I am creating link for note {string}", (noteTopic: string) => {
  start.jumpToNotePage(noteTopic).startSearchingAndLinkNote()
})

function makingLink(cy, fromNoteTopic: string, linkType: string, toNoteTopic: string) {
  start.jumpToNotePage(fromNoteTopic).startSearchingAndLinkNote()
  cy.searchNote(toNoteTopic, ["All My Notebooks And Subscriptions"])
  cy.clickButtonOnCardBody(toNoteTopic, "Select")
  cy.clickRadioByLabel(linkType)
}

When(
  "I link note {string} as {string} note {string}",
  (fromNoteTopic: string, linkType: string, toNoteTopic: string) => {
    makingLink(cy, fromNoteTopic, linkType, toNoteTopic)
    cy.findByRole("button", { name: "Create Link" }).click()
  },
)

When(
  "I link note {string} as {string} note {string} and move under it",
  (fromNoteTopic: string, linkType: string, toNoteTopic: string) => {
    makingLink(cy, fromNoteTopic, linkType, toNoteTopic)
    cy.formField("Also Move To Under Target Note").check()
    cy.findByRole("button", { name: "Create Link" }).click()
    cy.findByRole("button", { name: "OK" }).click()
  },
)

When(
  "there is {string} link between note {string} and {string}",
  (linkType: string, fromNoteTopic: string, toNoteTopic: string) => {
    cy.testability().seedLink(linkType, fromNoteTopic, toNoteTopic)
  },
)

When("I should see the source note as {string}", (noteTopic: string) => {
  cy.findByText(noteTopic, { selector: "strong" }).should("be.visible")
})

When("I should see {string} as the possible duplicate", (noteTopicsAsString: string) => {
  cy.tick(500)
  cy.expectExactLinkTargets(noteTopicsAsString.commonSenseSplit(",").map((i: string) => i.trim()))
})

When(
  "I should see {string} as targets only when searching {string}",
  (noteTopicsAsString: string, searchKey: string) => {
    cy.searchNote(searchKey, [])
    cy.expectExactLinkTargets(noteTopicsAsString.commonSenseSplit(",").map((i: string) => i.trim()))
  },
)

When(
  "I should see {string} as targets only when searching in all my notebooks {string}",
  (noteTopicsAsString: string, searchKey: string) => {
    cy.searchNote(searchKey, ["All My Notebooks And Subscriptions"])
    cy.expectExactLinkTargets(noteTopicsAsString.commonSenseSplit(",").map((i: string) => i.trim()))
  },
)

When(
  "I should see note cannot be found when searching in all my notebooks {string}",
  (searchKey: string) => {
    cy.searchNote(searchKey, ["All My Notebooks And Subscriptions"])
    cy.findByText("No matching notes found.").should("be.visible")
  },
)

Then(
  "On the current page, I should see {string} has link {string} {string}",
  (noteTopic: string, linkType: string, targetNoteTopics: string) => {
    const targetNoteTopicsList = targetNoteTopics.commonSenseSplit(",")
    cy.findByText(targetNoteTopics.commonSenseSplit(",").pop(), {
      selector: ".link-title",
    })
    const linksForNoteFound: string[] = []
    cy.findAllByRole("button", { name: linkType })
      .parent()
      .parent()
      .each(($link) => {
        cy.wrap($link).within(() => {
          linksForNoteFound.push($link.text())
        })
      })
      .then(() => {
        expect(targetNoteTopicsList.every((linkItem) => linksForNoteFound.includes(linkItem))).to.be
          .true
      })
  },
)

Then("I should see {string} has no link to {string}", (noteTopic: string, targetTitle: string) => {
  start.jumpToNotePage(noteTopic)
  cy.findByText(targetTitle).should("not.exist")
})

Then(
  "I change the link from {string} to {string} to {string}",
  (noteTopic: string, targetTitle: string, linkType: string) => {
    start.jumpToNotePage(noteTopic)
    cy.changeLinkType(targetTitle, linkType)
  },
)

Then("I should be able to delete the link", () => {
  cy.findByRole("button", { name: "Delete" }).click()
})

Then("I delete the link from {string} to {string}", (noteTopic: string, targetTitle: string) => {
  cy.pageIsNotLoading()
  start.jumpToNotePage(noteTopic)
  cy.clickLinkNob(targetTitle)
  cy.findByRole("button", { name: "Delete" }).click()
  cy.findByRole("button", { name: "Cancel" }).click()
  cy.clickLinkNob(targetTitle)
  cy.findByRole("button", { name: "Delete" }).click()
  cy.findByRole("button", { name: "OK" }).click()
  cy.contains(targetTitle).should("not.exist")
  cy.findNoteTopic(noteTopic) // remain on the same note page
})
