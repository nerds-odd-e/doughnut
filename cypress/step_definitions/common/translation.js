/// <reference types="cypress" />
// @ts-check

import {
  And,
  Before,
  Given,
  Then,
  When,
} from "cypress-cucumber-preprocessor/steps";

Then("I should see button with text or title as {string}", (btnTextOrTitle) => {
  cy.expectButtonWithTextOrTitle(btnTextOrTitle);
});

When("I click on button with text or title as {string}", (btnTextOrTitle) => {
  cy.clickButtonWithTextOrTitle(btnTextOrTitle);
});

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