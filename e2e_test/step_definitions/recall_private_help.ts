/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'
import { assumeMemoryTrackerPage } from '../start/pageObjects/memoryTrackerPage'
import { assumePrivateRecallHelp } from '../start/pageObjects/privateRecallHelp'

When('I ask private recall help {string}', (message: string) => {
  start.assumeAnsweredQuestionPage().getPrivateHelp().ask(message)
})

When(
  "I confirm the tutor's proposal to exclude my attempt because {string}",
  (reason: string) => {
    assumePrivateRecallHelp().confirmExclusionProposal(reason)
  }
)

Then(
  'my answered question should read as excluded for an ambiguous question because {string}',
  (reason: string) => {
    start.assumeAnsweredQuestionPage().expectAttemptExcludedAsAmbiguous(reason)
  }
)

When('I leave private recall help and view the memory tracker', () => {
  assumePrivateRecallHelp().close()
  start.assumeAnsweredQuestionPage().viewMemoryTracker()
})

Then(
  'the recall history should show the {string} attempt as excluded because {string}',
  (outcome: string, reason: string) => {
    assumeMemoryTrackerPage().expectExcludedAttemptWithOutcome(reason, outcome)
  }
)

Then('the next recall time should be the assimilation time', () => {
  assumeMemoryTrackerPage().expectNextRecallAtAssimilationTime()
})

Then(
  'private recall help for that attempt should still show it as excluded because {string}',
  (reason: string) => {
    assumeMemoryTrackerPage()
      .getPrivateHelpForExcludedAttempt()
      .expectAttemptExcludedAsAmbiguous(reason)
  }
)
