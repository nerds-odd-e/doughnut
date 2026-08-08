/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import { commonSenseSplit } from 'support/string_util'
import start from '../start'

When(
  'I commission a learning session for notebook {string}',
  (notebookTitle: string) => {
    start
      .recall()
      .navigateToRecallPage()
      .commissionLearningSession(notebookTitle)
  }
)

Then(
  'the learning session request should list session items for notes {string}',
  (noteTitles: string) => {
    start
      .recall()
      .assumeRecallPage()
      .expectLearningSessionRequestListsNotes(noteTitles)
  }
)

Then(
  'the learning session request should include the learning status of {string}',
  (noteTitle: string) => {
    start
      .recall()
      .assumeRecallPage()
      .expectLearningSessionRequestIncludesLearningStatus(noteTitle)
  }
)

Then(
  'the learning session request should include the expected learning content {string}',
  (content: string) => {
    start
      .recall()
      .assumeRecallPage()
      .expectLearningSessionRequestIncludesContent(content)
  }
)

Then(
  'the learning session request should instruct the tutor to report one score per session item',
  () => {
    start
      .recall()
      .assumeRecallPage()
      .expectLearningSessionRequestIncludesRubric()
  }
)

Then("the learning session should be awaiting the tutor's report", () => {
  start.recall().assumeRecallPage().expectLearningSessionAwaitingReport()
})
