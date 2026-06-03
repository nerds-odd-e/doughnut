import type { MemoryTrackerController, RecallsController } from 'doughnut-api'
import makeMe from 'doughnut-test-fixtures/makeMe'
import type { MockInstance } from 'vitest'
import { baseNoteTimes } from './recallJustReviewInteractive.fixtures.js'
import { deferred } from './recallInteractiveShared.js'

export type RecallJustReviewSpies = {
  recallingSpy: MockInstance
  showMemoryTrackerSpy: MockInstance
  markAsRecalledSpy: MockInstance
}

export function mockShowMemoryTrackerCardForRealm(
  spies: RecallJustReviewSpies,
  noteRealm: ReturnType<typeof makeMe.aNoteRealm.please>
) {
  spies.showMemoryTrackerSpy.mockResolvedValue({
    data: makeMe.aMemoryTracker
      .nextRecallAt('2026-06-01T00:00:00Z')
      .ofNote(noteRealm)
      .please(),
  } as Awaited<ReturnType<typeof MemoryTrackerController.showMemoryTracker>>)
}

export function mockMarkAsRecalledCounting(spies: RecallJustReviewSpies) {
  const markAsRecalledCount = { n: 0 }
  spies.markAsRecalledSpy.mockImplementation(async () => {
    markAsRecalledCount.n += 1
    return {
      data: makeMe.aMemoryTracker.please(),
    } as Awaited<ReturnType<typeof MemoryTrackerController.markAsRecalled>>
  })
  return markAsRecalledCount
}

export function mockAlphaBetaNoteCards(spies: RecallJustReviewSpies) {
  spies.showMemoryTrackerSpy.mockImplementation(
    async (opts: { path: { memoryTracker: number } }) => {
      const id = opts.path.memoryTracker
      const title = id === 1 ? 'Alpha' : 'Beta'
      const noteRealm = makeMe.aNoteRealm
        .title(title)
        .content('body')
        .createdAt(baseNoteTimes.createdAt)
        .updatedAt(baseNoteTimes.updatedAt)
        .please()

      return {
        data: makeMe.aMemoryTracker
          .nextRecallAt('2026-06-01T00:00:00Z')
          .ofNote(noteRealm)
          .please(),
      } as Awaited<ReturnType<typeof MemoryTrackerController.showMemoryTracker>>
    }
  )
}

export function setupTwoDueJustReviewItemsMocks(spies: RecallJustReviewSpies) {
  spies.recallingSpy.mockImplementation(async () => {
    return {
      data: makeMe.aDueMemoryTrackersList
        .totalAssimilatedCount(0)
        .toRepeat([
          { memoryTrackerId: 1, spelling: false },
          { memoryTrackerId: 2, spelling: false },
        ])
        .please(),
    } as Awaited<ReturnType<typeof RecallsController.recalling>>
  })
  mockAlphaBetaNoteCards(spies)
}

/** First window: two due trackers; extended `dueindays=3` call returns empty list. */
export function mockTwoDueThenEmptyExtendedRecalling(
  spies: RecallJustReviewSpies
) {
  let recallingN = 0
  spies.recallingSpy.mockImplementation(
    async (opts: Parameters<typeof RecallsController.recalling>[0]) => {
      recallingN += 1
      const due = opts.query.dueindays ?? 0
      const trackers =
        recallingN === 1 && due === 0
          ? [
              { memoryTrackerId: 1, spelling: false },
              { memoryTrackerId: 2, spelling: false },
            ]
          : []
      return {
        data: makeMe.aDueMemoryTrackersList
          .totalAssimilatedCount(0)
          .toRepeat(trackers)
          .please(),
      } as Awaited<ReturnType<typeof RecallsController.recalling>>
    }
  )
  mockAlphaBetaNoteCards(spies)
}

export function mockDeferredMarkAsRecalled(spies: RecallJustReviewSpies) {
  const { promise, resolve } =
    deferred<
      Awaited<ReturnType<typeof MemoryTrackerController.markAsRecalled>>
    >()
  spies.markAsRecalledSpy.mockImplementation(() => promise)
  return {
    resolveMark: (
      value: Awaited<ReturnType<typeof MemoryTrackerController.markAsRecalled>>
    ) => resolve(value),
  }
}

export function mockShowMemoryTrackerSecondCardDelayed(
  spies: RecallJustReviewSpies,
  firstNoteRealm: ReturnType<typeof makeMe.aNoteRealm.please>,
  secondNoteRealm: ReturnType<typeof makeMe.aNoteRealm.please>
) {
  const { promise, resolve } =
    deferred<
      Awaited<ReturnType<typeof MemoryTrackerController.showMemoryTracker>>
    >()
  spies.showMemoryTrackerSpy.mockImplementation(
    async (opts: { path: { memoryTracker: number } }) => {
      const id = opts.path.memoryTracker
      if (id === 1) {
        return {
          data: makeMe.aMemoryTracker
            .nextRecallAt('2026-06-01T00:00:00Z')
            .ofNote(firstNoteRealm)
            .please(),
        } as Awaited<
          ReturnType<typeof MemoryTrackerController.showMemoryTracker>
        >
      }
      if (id === 2) {
        return promise
      }
      throw new Error(`unexpected memoryTracker ${String(id)}`)
    }
  )
  return {
    resolveSecondCard: (
      value: Awaited<
        ReturnType<typeof MemoryTrackerController.showMemoryTracker>
      >
    ) => resolve(value),
  }
}
