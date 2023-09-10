/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given } from "@badeball/cypress-cucumber-preprocessor"
import pageObjects from 'page_objects';

Given(
  "a developer should be able to download the training data with {int} record",
  (count: number) => {
    pageObjects.loginAsAdminAndGoToAdminDashboard()
    cy.findByRole("button", { name: "Download" }).click()
    const downloadsFolder = Cypress.config("downloadsFolder")
    cy.readFile(`${downloadsFolder}/trainingdata.txt`)
      .then((content) => (content.match(/messages/g) || []).length)
      .should("eq", count)
  },
)
