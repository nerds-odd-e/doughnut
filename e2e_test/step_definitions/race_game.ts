import { Given } from '@badeball/cypress-cucumber-preprocessor'

Given('I am at the race game', () => {
  cy.visit('/d/race')
})
