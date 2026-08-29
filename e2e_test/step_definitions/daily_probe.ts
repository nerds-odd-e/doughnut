/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Then('I should see the Daily probe instruction', () => {
  start.recall().assumeRecallPage().expectDailyProbeInstruction()
})

Then('I should not see the Daily probe instruction', () => {
  start.recall().assumeRecallPage().expectNoDailyProbeInstruction()
})

When('I complete the Daily probe', () => {
  start.recall().assumeRecallPage().completeDailyProbe()
})

Then('I should see Daily probe speed {string}', (speed: string) => {
  start.recall().assumeRecallPage().expectDailyProbeSpeed(speed)
})

When('I continue from the Daily probe', () => {
  start.recall().assumeRecallPage().continueFromDailyProbe()
})

Then('I should see ordinary recall', () => {
  start.recall().assumeRecallPage().expectCurrentQuestion()
})
