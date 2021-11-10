/// <reference types="cypress" />
// @ts-check

import {
  And,
  Before,
  Given,
  Then,
  When,
} from "cypress-cucumber-preprocessor/steps";

Then("Note title will be shown {string} version", (title) => {
  cy.expectNoteTitle(title);
});

Then("I should see button {string}", (lang) => {
  cy.expectTranslationButtonLang(lang);
});

Then("I should see button with text or title as {string}", (btnTextOrTitle) => {
  cy.expectButtonWithTextOrTitle(btnTextOrTitle);
});

When("I click on button with text or title as {string}", (btnTextOrTitle) => {
  cy.clickButtonWithTextOrTitle(btnTextOrTitle);
});
