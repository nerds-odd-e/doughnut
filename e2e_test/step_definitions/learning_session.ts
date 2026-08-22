/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import type { DataTable } from '@cucumber/cucumber'
import { LearningSessionController } from '@generated/doughnut-backend-api/sdk.gen'
import start from '../start'

const SESSION_ITEM_GRADES_OPEN_TAG = '<session_item_grades>'
const SESSION_ITEM_GRADES_CLOSE_TAG = '</session_item_grades>'
const SESSION_ITEM_FEEDBACK_OPEN_TAG = '<session_item_feedback>'
const SESSION_ITEM_FEEDBACK_CLOSE_TAG = '</session_item_feedback>'

function recordLearningSessionForNotebook(
  notebookTitle: string,
  reportMarkdown: string
) {
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

Given(
  'I have recorded a learning session for notebook {string} on day {int} with grades:',
  (notebookTitle: string, day: number, dataTable: DataTable) => {
    start.testability().timeTravelTo(day, 9)
    const lines = dataTable.hashes().map((row) => `${row.Note}: ${row.Grade}`)
    const reportMarkdown = `# Learning Session Report\n\n${SESSION_ITEM_GRADES_OPEN_TAG}\n${lines.join('\n')}\n${SESSION_ITEM_GRADES_CLOSE_TAG}\n`
    recordLearningSessionForNotebook(notebookTitle, reportMarkdown)
  }
)

Given(
  'I have recorded a learning session for notebook {string} on day {int}, {int} hour with feedback:',
  (notebookTitle: string, day: number, hour: number, dataTable: DataTable) => {
    start.testability().timeTravelTo(day, hour)
    const items = dataTable
      .hashes()
      .map((row) => `### ${row.Note}\nGrade: ${row.Grade}\n${row.Text}`)
      .join('\n\n')
    const reportMarkdown = `# Learning Session Report\n\n${SESSION_ITEM_FEEDBACK_OPEN_TAG}\n${items}\n${SESSION_ITEM_FEEDBACK_CLOSE_TAG}\n`
    recordLearningSessionForNotebook(notebookTitle, reportMarkdown)
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
  'the recorded Feedback for notebook {string} should be shown',
  (_notebookTitle: string) => {
    start.recall().assumeRecallPage().expectLearningSessionReportRecorded()
  }
)

function commissionedMemoryTracker(noteTitle: string) {
  return start.testability().memoryTrackerForNote(noteTitle, 'COMMISSIONED')
}

Then(
  'the commissioned memory tracker for {string} should have recall count {int}',
  (noteTitle: string, recallCount: number) => {
    commissionedMemoryTracker(noteTitle).then((tracker) => {
      expect(
        tracker.recallCount,
        `commissioned recall count for ${noteTitle}`
      ).to.eq(recallCount)
    })
  }
)

Then(
  'the commissioned memory tracker for {string} should have tutor feedback grade {int}',
  (noteTitle: string, grade: number) => {
    commissionedMemoryTracker(noteTitle).then((tracker) => {
      expect(
        tracker.latestTutorFeedbackGrade,
        `tutor feedback grade for ${noteTitle}`
      ).to.eq(grade)
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
  'the learning session request should include the tutoring status of {string}',
  (noteTitle: string) => {
    start
      .recall()
      .assumeRecallPage()
      .expectLearningSessionRequestIncludesTutoringStatus(noteTitle)
  }
)

Then(
  'the learning session request should include focus note with note body {string}',
  (content: string) => {
    start
      .recall()
      .assumeRecallPage()
      .expectLearningSessionRequestIncludesFocusNoteBody(content)
  }
)

Then(
  'the learning session request should include related notes with note body {string}',
  (content: string) => {
    start
      .recall()
      .assumeRecallPage()
      .expectLearningSessionRequestIncludesRelatedNoteBody(content)
  }
)

Then(
  'the learning session request should instruct the tutor to report a grade and descriptive text per session item',
  () => {
    start
      .recall()
      .assumeRecallPage()
      .expectLearningSessionRequestInstructsDescriptiveFeedback()
  }
)

Then(
  'the learning session request should include dated Feedbacks for {string}:',
  (noteTitle: string, dataTable: DataTable) => {
    start
      .recall()
      .assumeRecallPage()
      .expectLearningSessionRequestIncludesDatedFeedbacks(
        noteTitle,
        dataTable.hashes()
      )
  }
)
