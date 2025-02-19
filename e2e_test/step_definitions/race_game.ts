import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'

Given('I am at the race game', () => {
  cy.visit('/d/race')
})

When('I choose to go normal for this round', () => {
  cy.contains('button', 'GO NORMAL').click()
})
