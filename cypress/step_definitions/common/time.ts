/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../../support" />
// @ts-check

import {
  And,
  Before,
  Given,
  Then,
  When,
} from "cypress-cucumber-preprocessor/steps";

Given("I let the server to time travel to {int} hours ago", (hours) => {
  cy.timeTravelRelativeToNow(-hours);
});
