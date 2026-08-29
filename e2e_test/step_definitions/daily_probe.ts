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

Then('I should see Daily probe accuracy {string}', (accuracy: string) => {
  start.recall().assumeRecallPage().expectDailyProbeAccuracy(accuracy)
})

Then('I should see Daily probe lapses {string}', (lapses: string) => {
  start.recall().assumeRecallPage().expectDailyProbeLapses(lapses)
})

Then('I should see Daily probe variability {string}', (variability: string) => {
  start.recall().assumeRecallPage().expectDailyProbeVariability(variability)
})

When('I continue from the Daily probe', () => {
  start.recall().assumeRecallPage().continueFromDailyProbe()
})

Then('I should see ordinary recall', () => {
  start.recall().assumeRecallPage().expectCurrentQuestion()
})
