/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, When, Then } from "@badeball/cypress-cucumber-preprocessor";
import start from "../start";
import { DataTable } from "@cucumber/cucumber";

Given(
  "I have {int} positive feedbacks and {int} negative feedbacks",
  (positive: number, negative: number) => {
    cy.get(
      'a[title="send this question for fine tuning the question generation model"]',
    ).click();
    for (let i = 0; i < positive; i++) {
      cy.findByRole("button", { name: "👍 Good" }).click();
    }

  },
);

Then(
  "I should see the error message {string}",
  (message: string) => {
    cy.findByText(message).should("exist");
  },
);
