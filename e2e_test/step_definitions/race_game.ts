import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Given('I am at the beginning of the race game as {string}', (name: string) => {
  start.routerToRaceGamePage().joinAs(name)
})

When('I choose to go normal for this round', () => {
  start.assumeRaceGamePage().rollDice()
})

When('I reset the game', () => {
  start.assumeRaceGamePage().resetGame()
})

Then(
  'my car should move no further than {int} steps at round {int}',
  (steps: number, round: number) => {
    start.assumeRaceGamePage().expectCarPosition(steps, round)
  }
)

Then('I should see a car belongs to {string}', (name: string) => {
  start.assumeRaceGamePage().expectCarBelongsTo(name)
})
