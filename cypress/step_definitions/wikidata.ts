/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from "@badeball/cypress-cucumber-preprocessor"

Given("there is a wikidata record {string}", (term) => {
  cy.log(term)
})

When("I fetch for {string} from Wikidata",(term) => {
  cy.request(`/api/wikidata/${term}`).then(resp => {
    cy.wrap(resp.body).as("wikidata_payload")
  })
})

Then("I can get a payload {string}", (expectedPayload) => {
  cy.get("@wikidata_payload").then(payload => {
    expect(payload).to.equal(expectedPayload)
  })
})
