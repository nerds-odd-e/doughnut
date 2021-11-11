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

Then("Note title will be shown {string} version", (title) => {
  cy.expectNoteTitle(title);
});

Then("I should see button {string}", (lang) => {
  cy.expectTranslationButtonLang(lang);
});

When("I click on the translation button {string}", (lang) => {
  const buttonText = `Translate to ${lang}`;
  cy.clickTranslationButton(buttonText);
});

Then("Note title will be shown {string} version", (title) => {
  cy.expectNoteTitle(title);
});

Then("I should see button {string}", (lang) => {
  cy.expectTranslationButtonLang(lang);
});

When("I click on the translation button {string}", (lang) => {
  const buttonText = `Translate to ${lang}`;
  cy.clickTranslationButton(buttonText);
});

Then("Note title will be shown {string} version", (title) => {
  cy.expectNoteTitle(title);
});

Then("I should see button {string}", (lang) => {
  cy.expectTranslationButtonLang(lang);
});
