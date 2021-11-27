/// <reference types="cypress" />
// @ts-check

import {
  And,
  Before,
  Given,
  Then,
  When,
} from "cypress-cucumber-preprocessor/steps";

// View Translation

Given("I switch language to {string}", (lang) => {
  const buttonText = `Translate to ${lang}`;
  cy.clickTranslationButton(buttonText);
});

When("I switch language to {string}", (lang) => {
  const buttonText = `Translate to ${lang}`;
  cy.clickTranslationButton(buttonText);
});

Then("Note title on the page should be {string}", (title) => {
  cy.expectNoteTitle(title);
});

And("Note description on the page should be {string}", (description) => {
  cy.expectText(description);
});

Given("I jump to {string} tab", (noteTab) => {
  cy.clickNoteTab(noteTab);
});