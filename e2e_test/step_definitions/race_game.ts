/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../support" />

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Given('I visit the race game page', () => {
  start.routerToRaceGamePage()
})

When('I roll the dice', () => {
  start.routerToRaceGamePage().rollDice()
})

When('I reset the game', () => {
  start.routerToRaceGamePage().resetGame()
})

Then('I should see my car at position {int}', (position: number) => {
  start.routerToRaceGamePage().expectCarPosition(position)
})

Then('I should see the dice outcome is {int}', (outcome: number) => {
  start.routerToRaceGamePage().expectDiceOutcome(outcome)
})

Then('I should see that I have won the game', () => {
  start.routerToRaceGamePage().expectGameWon()
})

Then('I should see round count is {int}', (count: number) => {
  start.routerToRaceGamePage().expectRoundCount(count)
})
