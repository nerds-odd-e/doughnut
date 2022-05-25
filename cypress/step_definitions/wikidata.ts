/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />
// @ts-check

import { And, Given, Then, When } from "@badeball/cypress-cucumber-preprocessor"

When("I associate {string} with wikidata id {string}", (noteTitle, wikiDataId) => {
  // UI steps
})

Then("I should see association confirmation message {string}", (message) => {
  // TODO
})
