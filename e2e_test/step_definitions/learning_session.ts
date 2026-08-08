/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { Given, Then, When } from '@badeball/cypress-cucumber-preprocessor'
import type { DataTable } from '@cucumber/cucumber'
import { NoteController } from '@generated/doughnut-backend-api/sdk.gen'
import start from '../start'

When(
  'I commission a learning session for notebook {string}',
  (notebookTitle: string) => {
    start.recall().visitRecallPage().commissionLearningSession(notebookTitle)
  }
)

Given(
  'I have commissioned a learning session for notebook {string} on day {int} with session items for notes {string}',
  (notebookTitle: string, day: number, _noteTitles: string) => {
    start.testability().timeTravelTo(day, 9)
    start
      .recall()
      .navigateToRecallPage()
      .commissionLearningSession(notebookTitle)
  }
)

Given(
  'I have recorded a learning session for notebook {string} on day {int} with scores:',
  (notebookTitle: string, day: number, dataTable: DataTable) => {
    start.testability().timeTravelTo(day, 9)
    start.recall().visitRecallPage().commissionLearningSession(notebookTitle)
    const lines = dataTable.hashes().map((row) => `${row.Note}: ${row.Score}`)
    const reportMarkdown = `# Learning Session Report\n\n${lines.join('\n')}\n`
    start
      .recall()
      .assumeRecallPage()
      .recordLearningSessionReport(reportMarkdown)
      .expectLearningSessionRecorded()
  }
)

When(
  'I record the learning session report for the learning session of notebook {string}:',
  (_notebookTitle: string, reportMarkdown: string) => {
    start
      .recall()
      .assumeRecallPage()
      .recordLearningSessionReport(reportMarkdown)
  }
)

Then(
  'the learning session for notebook {string} should be marked as recorded',
  (_notebookTitle: string) => {
    start.recall().assumeRecallPage().expectLearningSessionRecorded()
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
  'the learning session request should list session items for only notes {string}',
  (noteTitles: string) => {
    start
      .recall()
      .assumeRecallPage()
      .expectLearningSessionRequestListsOnlyNotes(noteTitles)
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

Then(
  'I should see tutor feedback score {int} from a learning session for the memory tracker of note {string}',
  (score: number, noteTitle: string) => {
    start.jumpToNotePage(noteTitle).moreOptions().openAssimilationSettings()
    start.assumeAssimilationPage().expectTutorFeedbackScore(score)
  }
)
