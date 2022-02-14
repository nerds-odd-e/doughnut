/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../../support" />
// @ts-check

import {
  And,
  Before,
  After,
  Given,
  Then,
  When,
} from "cypress-cucumber-preprocessor/steps";

Before(() => {
  cy.cleanDBAndSeedData();
  cy.wrap(false).as('firstVisited')
});

Before({ tags: "@featureToggle" }, () => {
  cy.enableFeatureToggle(true)
})

Before({ tags: "@cleanDownloadFolder" }, () => {
  cy.cleanDownloadFolder()
})

Before({ tags: "@stopTime" }, () => {
  cy.clock()
})
