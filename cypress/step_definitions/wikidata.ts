/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { DataTable, Given, Then, When } from "@badeball/cypress-cucumber-preprocessor"

When("I associate the note {string} with wikidata id {string}", (title, wikiID) => {
  cy.associateNoteWithWikidataId(title, wikiID)
})

When("I need to confirm the association with different title {string}", (wikidataTitle: string) => {
  cy.findAllByText(wikidataTitle).should("exist")
  cy.findByRole("button", { name: "Confirm" }).click()
  cy.findWikiAssociationButton()
})

Then("I don't need to confirm the association with different title {string}", () => {
  cy.findWikiAssociationButton()
})

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
  "Wikidata.org entity {string} is a person from {string} and birthday is {string}",
  (wikidataId: string, countryId: string, birthday: string) => {
    cy.wikidataService().stubWikidataEntityPerson(wikidataId, countryId, birthday)
  },
)

Given(
  "Wikidata.org entity {string} is a location at {float}, {float}",
  (wikidataId: string, lat: number, lng: number) => {
    cy.wikidataService().stubWikidataEntityLocation(wikidataId, lat, lng)
  },
)

Given("The wikidata service is not available", () => {
  // The service should be mocked, but no stubbing is done
})

Then("I should see an error {string} on {string}", (message: string, field: string) => {
  cy.expectFieldErrorMessage(message, field)
})

Then(
  "the Wiki association of note {string} should link to {string}",
  (title: string, associationUrl: string) => {
    cy.findNoteTitle(title)
    cy.findWikiAssociationButton().expectALinkThatOpensANewWindowWithURL(associationUrl)
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

Then("I should see that the {string} becomes {string}", (field: string, value: string) => {
  cy.formField(field).should("have.value", value)
})

Then(
  "a map pointing to lat: {string}, lon: {string} is added to the note",
  (latitude: string, longitude: string) => {
    cy.expectAMapTo(latitude, longitude)
  },
)

Given(
  "the Wikidata.org entity {string} is written by authors with ID",
  (wikidataId: string, data: DataTable) => {
    cy.wikidataService().stubWikidataEntityBook(
      wikidataId,
      data.hashes().map((hash) => hash["Wikidata Id"]),
    )
  },
)
