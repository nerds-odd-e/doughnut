import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'

Given('I am at the race game', () => {
  cy.visit('/d/race')
})

Given('the round count is {int}', (roundCount: number) => {
  // We'll need to set up the initial state through the API
  cy.request('POST', '/api/race/reset', { playerId: 'test-player' })
})

When('the dice outcome is {int}', (diceOutcome: number) => {
})

Then('the car should move to position {int}', (position: number) => {
})

Then('the round count becomes {int}', (roundCount: number) => {
})
