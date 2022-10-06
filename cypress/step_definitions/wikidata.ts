/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from "@badeball/cypress-cucumber-preprocessor"

When("I associate the note {string} with wikidata id {string}", (title, wikiID) => {
  cy.associateNoteWithWikidataId(title, wikiID)
})

When("I need to confirm the association with different title {string}", (wikidataTitle: string) => {
  cy.findAllByText(wikidataTitle).should("exist")
  cy.findByRole("button", { name: "Confirm" }).click()
  cy.findByRole("button", { name: "Wikidata" })
})

Then("I don't need to confirm the association with different title {string}", () => {
  // no action needed
  cy.findByRole("button", { name: "Wikidata" })
})

Given(
  "Wikidata.org entity {string} is a human with date on birthday {string} and country of citizenship {string}",
  () => ({}),
)

Given(
  "Wikidata.org has an entity {string} with title {string} and link to wikipedia {string}",
  (wikidataId: string, wikidataTitle: string, wikipediaLink: string) => {
    cy.wikidataService().stubWikidataEntityQuery(wikidataId, wikidataTitle, wikipediaLink)
  },
)

Given(
  "Wikidata.org has an entity {string} with title {string}",
  (wikidataId: string, wikidataTitle: string) => {
    cy.wikidataService().stubWikidataEntityQuery(wikidataId, wikidataTitle, undefined)
  },
)

Given(
  "Wikidata.org entity {string} is a location at {float}, {float}",
  (wikidataId: string, lat: number, lng: number) => {
    cy.wikidataService().stubWikidataEntityLocation(wikidataId, lat, lng)
  },
)

Given("The wikidata service is not available", () => {
  // checking if the saved Wikidata service url is the real url, which indicate the service is mocked.
  // This test require the service to be mocked first.
  cy.get("@savedWikidataServiceUrl").then((url) => {
    expect(url).to.include("https://www.wikidata.org")
  })
})

Then("I should see an error {string} on {string}", (message: string, field: string) => {
  cy.expectFieldErrorMessage(message, field)
})

Then(
  "I should see the {string} icon beside title {string} linking to {string}",
  (buttonName: string, title: string, associationUrl: string) => {
    cy.findNoteTitle(title)
    cy.findPopupLink(buttonName, associationUrl)
  },
)

Given(
  "Wikidata search result always has {string} with ID {string}",
  (wikidataLabel: string, wikidataId: string) => {
    cy.wikidataService().stubWikidataSearchResult(wikidataLabel, wikidataId)
  },
)

When("I search with title {string} on Wikidata", (title: string) => {
  cy.focused().clear().type(title)
  cy.findByRole("button", { name: "Search on Wikidata" }).click()
})

When("I select wikidataID {string} from the Wikidata search result", (wikidataID: string) => {
  cy.get('select[name="wikidataSearchResult"]').select(wikidataID)
})

When("I create a note belonging to {string} with id {string}", (name: string, id: string) => {
  cy.clickAddChildNoteButton()
})

Then("I should see that the {string} becomes {string}", (field: string, value: string) => {
  cy.getFormControl(field).should("have.value", value)
})

Then(
  "a map pointing to {string}, {string} is added to the note",
  (longitude: string, latitude: string) => {
    cy.get("map").invoke("attr", "lon").should("eq", longitude)
    cy.get("map").invoke("attr", "lat").should("eq", latitude)
  },
)

Then("an identifying picture with the name {string} is shown in the note", (fileName: string) => {
  cy.get("picture").invoke("attr", "ref").should("contain", fileName)
})
