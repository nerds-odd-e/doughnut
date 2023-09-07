/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given } from "@badeball/cypress-cucumber-preprocessor"

Given("I attempt to export", () => {})

Given("I should return an empty JSONL file", () => {})

Given("a file with training data is produced", () => {
  cy.visit("/dev-training-data")
  cy.findByRole("button", { name: "Download" }).click()
  cy.readFile("cypress/downloads/trainingdata.txt").should("contain", "messages")
})

Given("that there are no questions marked good at all", () => {})
