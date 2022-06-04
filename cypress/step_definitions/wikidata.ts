/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from "@badeball/cypress-cucumber-preprocessor"

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
