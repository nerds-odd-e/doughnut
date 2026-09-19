/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Then, When } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

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
