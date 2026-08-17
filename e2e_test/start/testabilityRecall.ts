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
        tracker.lastRecalledAt,
        `expected spelling tracker for "${noteTitle}" to have Last Recall Time after credited recall`
      )
        .to.be.a('string')
        .and.not.equal('')
      expect(
        tracker.recallCount,
        `expected spelling tracker for "${noteTitle}" to have recall count after credited recall`
      ).to.be.at.least(1)
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
    return this.memoryTrackerForNote(noteTitle, 'SPELLING').then((tracker) => {
      cy.get<SpellingScheduleSnapshot>(
        `@${spellingScheduleAlias(noteTitle)}`
      ).then((recorded) => {
        expect(
          tracker.lastRecalledAt ?? '',
          `Last Recall Time for "${noteTitle}" should stay ${recorded.lastRecalledAt}`
        ).to.equal(recorded.lastRecalledAt)
        expect(
          tracker.nextRecallAt,
          `Next Recall Time for "${noteTitle}" should stay ${recorded.nextRecallAt}`
        ).to.equal(recorded.nextRecallAt)
        expect(
          String(tracker.recallCount ?? 0),
          `Recall Count for "${noteTitle}" should stay ${recorded.recallCount}`
        ).to.equal(recorded.recallCount)
      })
    })
  },
}
