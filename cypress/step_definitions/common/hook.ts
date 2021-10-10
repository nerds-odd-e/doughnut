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

Before(() => {
  cy.cleanDBAndSeedData();
});

Before({ tags: "@featureToggle"}, ()=> {
    cy.enableFeatureToggle(true)
})