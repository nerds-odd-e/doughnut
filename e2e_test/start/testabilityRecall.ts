/// <reference types="Cypress" />
// @ts-check
import type {
  MemoryTracker,
  NoteRecallInfo,
  RecallPrompt,
} from '@generated/doughnut-backend-api'
import {
  MemoryTrackerController,
  NoteController,
  RecallPromptController,
  RecallsController,
} from '@generated/doughnut-backend-api/sdk.gen'
import { unwrapData } from './unwrapApi'

type InjectedNoteIds = {
  getInjectedNoteIdByTitle(noteTitle: string): Cypress.Chainable<number>
}

type MemoryTrackerType = NonNullable<MemoryTracker['type']>

type RecallTestability = InjectedNoteIds & {
  memoryTrackerForNote(
    noteTitle: string,
    trackerType: MemoryTrackerType
  ): Cypress.Chainable<MemoryTracker>
}

type SpellingScheduleSnapshot = {
  lastRecalledAt: string
  nextRecallAt: string
  recallCount: string
}

const spellingScheduleAlias = (noteTitle: string) =>
  `recordedSpellingSchedule-${noteTitle}`

function expectSpellingScheduleAgainstRecorded(
  testability: RecallTestability,
  noteTitle: string,
  assertNextRecallAt: (
    tracker: MemoryTracker,
    recorded: SpellingScheduleSnapshot
  ) => void
) {
  return testability
    .memoryTrackerForNote(noteTitle, 'SPELLING')
    .then((tracker) => {
      cy.get<SpellingScheduleSnapshot>(
        `@${spellingScheduleAlias(noteTitle)}`
      ).then((recorded) => {
        expect(
          tracker.lastRecalledAt ?? '',
          `Last Recall Time for "${noteTitle}" should stay ${recorded.lastRecalledAt || 'N/A'}`
        ).to.equal(recorded.lastRecalledAt)
        expect(
          String(tracker.recallCount ?? 0),
          `Recall Count for "${noteTitle}" should stay ${recorded.recallCount}`
        ).to.equal(recorded.recallCount)
        assertNextRecallAt(tracker, recorded)
      })
    })
}

export const recallTestabilityMethods = {
  memoryTrackerForNote(
    this: InjectedNoteIds,
    noteTitle: string,
    trackerType: MemoryTrackerType
  ) {
    return this.getInjectedNoteIdByTitle(noteTitle).then((noteId) =>
      cy
        .wrap(NoteController.getNoteInfo({ path: { note: noteId } }), {
          log: false,
        })
        .then((response) => {
          const tracker = unwrapData<NoteRecallInfo>(
            response
          ).memoryTrackers?.find((candidate) => candidate.type === trackerType)
          expect(
            tracker,
            `expected a ${trackerType} memory tracker for "${noteTitle}"`
          ).to.exist
          return tracker as MemoryTracker
        })
    )
  },

  dueRecallPrompt() {
    const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone
    return cy
      .wrap(
        RecallsController.recalling({
          query: { timezone, dueindays: 0 },
        }),
        { log: false }
      )
      .then((dueMemoryTrackers) => {
        const trackerId = dueMemoryTrackers?.toRepeat?.[0]?.memoryTrackerId
        expect(trackerId, 'expected one due memory tracker for recall').to.exist
        return cy
          .wrap(
            MemoryTrackerController.getRecallPrompt({
              path: { memoryTracker: trackerId! },
            }),
            { log: false }
          )
          .then((response) => {
            const prompt = unwrapData<RecallPrompt>(response)
            expect(prompt?.id, 'expected a due recall prompt').to.exist
            return prompt
          })
      })
  },

  submitWrongMcqRecallAnswer(
    this: { dueRecallPrompt(): Cypress.Chainable<RecallPrompt> },
    wrongChoiceText: string
  ) {
    return this.dueRecallPrompt().then((recallPrompt) => {
      const choices = recallPrompt?.mcq?.responseChoices
      expect(choices, 'expected MCQ response choices').to.exist
      const choiceIndex = choices!.indexOf(wrongChoiceText)
      expect(
        choiceIndex,
        `expected choice "${wrongChoiceText}" in ${JSON.stringify(choices)}`
      ).to.be.at.least(0)
      return cy.wrap(
        RecallPromptController.answer({
          path: { recallPrompt: recallPrompt!.id },
          body: { choiceIndex, thinkingTimeMs: 1000 },
        }),
        { log: false }
      )
    })
  },

  creditSpellingRecallForNote(this: RecallTestability, noteTitle: string) {
    return this.memoryTrackerForNote(noteTitle, 'SPELLING').then((tracker) =>
      cy
        .wrap(
          MemoryTrackerController.getRecallPrompt({
            path: { memoryTracker: tracker.id },
          }),
          { log: false }
        )
        .then((promptResponse) => {
          const prompt = unwrapData<RecallPrompt>(promptResponse)
          expect(
            prompt?.id,
            `expected a recall prompt for spelling tracker of "${noteTitle}"`
          ).to.exist
          return cy.wrap(
            RecallPromptController.answerSpelling({
              path: { recallPrompt: prompt.id },
              body: { spellingAnswer: noteTitle, thinkingTimeMs: 1000 },
            }),
            { log: false }
          )
        })
    )
  },

  captureSpellingTrackerSchedule(this: RecallTestability, noteTitle: string) {
    return this.memoryTrackerForNote(noteTitle, 'SPELLING').then((tracker) => {
      expect(
        tracker.nextRecallAt,
        `expected spelling tracker for "${noteTitle}" to have Next Recall Time`
      )
        .to.be.a('string')
        .and.not.equal('')
      const snapshot: SpellingScheduleSnapshot = {
        lastRecalledAt: tracker.lastRecalledAt ?? '',
        nextRecallAt: tracker.nextRecallAt,
        recallCount: String(tracker.recallCount ?? 0),
      }
      cy.wrap(snapshot).as(spellingScheduleAlias(noteTitle))
    })
  },

  expectSpellingTrackerScheduleUnchanged(
    this: RecallTestability,
    noteTitle: string
  ) {
    return expectSpellingScheduleAgainstRecorded(
      this,
      noteTitle,
      (tracker, recorded) => {
        expect(
          tracker.nextRecallAt,
          `Next Recall Time for "${noteTitle}" should stay ${recorded.nextRecallAt}`
        ).to.equal(recorded.nextRecallAt)
      }
    )
  },

  expectSpellingTrackerBroughtForwardWithoutRecallCredit(
    this: RecallTestability,
    noteTitle: string
  ) {
    return expectSpellingScheduleAgainstRecorded(
      this,
      noteTitle,
      (tracker, recorded) => {
        const next = new Date(tracker.nextRecallAt).getTime()
        const before = new Date(recorded.nextRecallAt).getTime()
        expect(
          next,
          `Next Recall Time (${tracker.nextRecallAt}) for "${noteTitle}" should be earlier than recorded ${recorded.nextRecallAt}`
        ).to.be.lessThan(before)
      }
    )
  },
}
