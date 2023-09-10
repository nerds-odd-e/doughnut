/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given } from "@badeball/cypress-cucumber-preprocessor"

Given(
  "a developer should be able to download the training data with {int} record",
  (count: number) => {
    cy.visit("/dev-training-data")
    cy.findByRole("button", { name: "Download" }).click()
    const downloadsFolder = Cypress.config("downloadsFolder")
    cy.readFile(`${downloadsFolder}/trainingdata.txt`)
      .then((content) => (content.match(/messages/g) || []).length)
      .should("eq", count)
  },
)
