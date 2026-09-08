/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { When } from '@badeball/cypress-cucumber-preprocessor'

When("I keep the saved note across the other worktree's fixture reset", () => {
  cy.task('worktreeResetIsolationAfterSeed', null, {
    timeout: 180_000,
  })
})
