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

Then("Note title will be shown {string}", (title) => {
  cy.expectNoteTitle(title);
});

And("Note description will be shown {string}", (description) => {
  cy.expectText(description);
});

And("I should see translation button with language code {string}", (lang) => {
  cy.expectTranslationButtonLang(lang);
});

Given("I jump to {string} tab", (noteTab) => {
  cy.clickNoteTab(noteTab);
});

When("I jump to {string} tab", (noteTab) => {
  cy.clickNoteTab(noteTab);
});