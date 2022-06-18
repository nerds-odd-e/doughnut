/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { And, Given, Then, When } from "@badeball/cypress-cucumber-preprocessor"

When("I associate the note {string} with wikidata id {string}", (title, wikiID) => {
  cy.clickAssociateWikiDataButton(title, wikiID)
})

When("I need to confirm the association with different title {string}", (wikidataTitle: string) => {
  cy.findAllByText(wikidataTitle).should("exist")
  cy.findByRole("button", { name: "Confirm" }).click()
})

When("I don't need to confirm the association with different title {string}", () => {
  // no action needed
})

Given(
  "Wikidata.org has an entity {string} with {string} and {string}",
  (wikidataId: string, wikidataTitle: string, wikipediaLink: string) => {
    cy.stubWikidataEntityQuery(wikidataId, wikidataTitle, wikipediaLink)
  },
)

Given(
  "Wikidata.org has an entity {string} with {string}",
  (wikidataId: string, wikidataTitle: string) => {
    cy.stubWikidataEntityQuery(wikidataId, wikidataTitle, undefined)
  },
)

Given("The wikidata service is not available", () => {
  // checking if the saved Wikidata service url is the real url, which indicate the service is mocked.
  // This test require the service to be mocked first.
  cy.get("@savedWikidataServiceUrl").then((url) => {
    expect(url).to.include("https://www.wikidata.org")
  })
})

Then("I should see a message {string}", (message: string) => {
  cy.expectFieldErrorMessage(message)
})

Then("I should see the icon beside title linking to {string}", (associationUrl: string) => {
  cy.window().then((win) => {
    const popupWindowStub = { location: { href: undefined }, focus: cy.stub() }
    cy.stub(win, "open").as("open").returns(popupWindowStub)
    cy.findByRole("button", { name: "Wikidata" }).click()
    cy.get("@open").should("have.been.calledWith", "")
    // using a callback so that cypress can wait until the stubbed value is assigned
    cy.wrap(() => popupWindowStub.location.href)
      .should((cb) => expect(cb()).equal(associationUrl))
      .then(() => {
        expect(popupWindowStub.focus).to.have.been.called
      })
  })
})

When("I associate the note {string} with a new wikidata id {string}", (title, wikiID) => {
  cy.clickAssociateWikiDataButton(title, wikiID)
  cy.findByRole("button", { name: "Confirm" }).click()
})

And("When I revisit the associate wikidata dialog", () => {
  cy.clickNoteToolbarButton("associate wikidata")
})

Then(
  "I should see that the placeholder containing the new wikidata id {string}",
  (placeholderText: string) => {
    cy.get('input[name="wikidataID"]')
      .invoke("attr", "placeholder")
      .then((text) => {
        expect(text).to.equal(placeholderText)
      })
  },
)

Given(
  "I have a note with title {string} associated with wikidata id {string}",
  (noteTitle: string, wikidataId: string) => {
    cy.seedNotes([{ title: noteTitle, wikidataId: wikidataId }])
  },
)

Given(
  "Wikidata has search result for {string} with wikidata ID {string}",
  (wikidataLabel: string, wikidataId: string) => {
    cy.stubWikidataSearchResult(wikidataLabel, wikidataId)
  },
)

And("I type {string} in the title and search on Wikidata", (title) => {
  cy.focused().clear().type(title)
  cy.findByRole("button", { name: "Search on Wikidata" }).click()
})

And(
  "I select {string} with wikidataID {string} from the Wikidata search result",
  (wikidataEntry: string, wikidataID: string) => {
    cy.get('select[name="wikidataSearchResult"]').select(wikidataID)
  },
)

And("I confirm that I want to replace the current title with the title from Wikidata", () => {
  cy.get('input[name="acceptSuggestion"]').click()
})

Then("I should see that the title is automatically populated with {string}", (title: string) => {
  cy.assertInputElementValue("title", title)
})

Then(
  "I should see that the Wikidata ID is automatically populated with {string}",
  (wikidataID: string) => {
    cy.assertInputElementValue("wikidataID", wikidataID)
  },
)
