/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import type { DataTable } from '@cucumber/cucumber'
import {
  LearningSessionController,
  NoteController,
} from '@generated/doughnut-backend-api/sdk.gen'
import start from '../start'

const SESSION_ITEM_SCORES_OPEN_TAG = '<session_item_scores>'
const SESSION_ITEM_SCORES_CLOSE_TAG = '</session_item_scores>'

Given(
  'I have recorded a learning session for notebook {string} on day {int} with scores:',
  (notebookTitle: string, day: number, dataTable: DataTable) => {
    start.testability().timeTravelTo(day, 9)
    const lines = dataTable.hashes().map((row) => `${row.Note}: ${row.Score}`)
    const reportMarkdown = `# Learning Session Report\n\n${SESSION_ITEM_SCORES_OPEN_TAG}\n${lines.join('\n')}\n${SESSION_ITEM_SCORES_CLOSE_TAG}\n`
    const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone
    start
      .testability()
      .getNotebookIdByName(notebookTitle)
      .then((notebookId) =>
        cy.wrap(
          LearningSessionController.record({
            body: { notebookId, reportMarkdown },
            query: { timezone },
          }),
          { log: false }
        )
      )
    start.recall().visitRecallPage()
  }
)

When(
  'I open the learning session request for notebook {string}',
  (notebookTitle: string) => {
    start.recall().visitRecallPage().openLearningSessionRequest(notebookTitle)
  }
)

When('I record the learning session report:', (reportMarkdown: string) => {
  start.recall().assumeRecallPage().recordLearningSessionReport(reportMarkdown)
})

Then(
  'the learning session for notebook {string} should be marked as recorded',
  (_notebookTitle: string) => {
    start.recall().assumeRecallPage().expectLearningSessionReportRecorded()
  }
)

Then(
  'the commissioned memory tracker for {string} should have recall count {int}',
  (noteTitle: string, recallCount: number) => {
    start
      .testability()
      .getInjectedNoteIdByTitle(noteTitle)
      .then((noteId) =>
        cy.wrap(NoteController.getNoteInfo({ path: { note: noteId } }), {
          log: false,
        })
      )
      .then((noteInfo) => {
        const commissioned = noteInfo?.memoryTrackers?.find(
          (tracker) => tracker.type === 'COMMISSIONED'
        )
        expect(
          commissioned?.recallCount,
          `commissioned recall count for ${noteTitle}`
        ).to.eq(recallCount)
      })
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
  'the learning session request should include focus context with note body {string}',
  (content: string) => {
    start
      .recall()
      .assumeRecallPage()
      .expectLearningSessionRequestIncludesFocusContextNoteBody(content)
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

Then(
  'no learning session should exist for notebook {string}',
  (notebookTitle: string) => {
    start
      .recall()
      .assumeRecallPage()
      .expectNoLearningSessionForNotebook(notebookTitle)
  }
)

Then(
  'I should see tutor feedback score {int} from a learning session for the memory tracker of note {string}',
  (score: number, noteTitle: string) => {
    start.jumpToNotePage(noteTitle).moreOptions().openAssimilationSettings()
    start.assumeAssimilationPage().expectTutorFeedbackScore(score)
  }
)
