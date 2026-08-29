/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

function answerSlowlyOnDay(day: number, thinkingTimeMs: number) {
  start.testability().backendTimeTravelTo(day, 8)
  start.testability().submitWrongMcqRecallAnswer('to sleep', thinkingTimeMs)
}

When(
  'I answer {string} slowly with thinking time {int} ms over {int} days since day {int}',
  (
    _noteTitle: string,
    thinkingTimeMs: number,
    numDays: number,
    startDay: number
  ) => {
    for (let i = 0; i < numDays; i++) {
      answerSlowlyOnDay(startDay + i, thinkingTimeMs)
    }
  }
)

When(
  'on day {int} I answer {string} slowly with thinking time {int} ms',
  (day: number, _noteTitle: string, thinkingTimeMs: number) => {
    answerSlowlyOnDay(day, thinkingTimeMs)
  }
)

When('I visit my recall stats', () => {
  start.visitRecallStatsPage()
})

Then("I should see today's pace is slower than usual", () => {
  start.recallStatsPage().expectPaceTileContains('slower than usual')
})

Then('I should see the Daily probe trend', () => {
  start.recallStatsPage().expectDailyProbeTrend()
})

Then('I should not see the Daily probe trend', () => {
  start.recallStatsPage().expectNoDailyProbeTrend()
})

Then('I should see empty recall stats', () => {
  start.recallStatsPage().expectEmptyRecallStats()
})

Then(
  'I should see {int} day(s) on the Daily probe speed trend',
  (days: number) => {
    start.recallStatsPage().expectDailyProbeSpeedTrendDays(days)
  }
)

When('I view the last 30 days of trends', () => {
  start.recallStatsPage().viewTrendWindow(30)
})
