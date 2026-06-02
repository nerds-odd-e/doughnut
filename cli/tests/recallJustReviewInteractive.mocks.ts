import type { MemoryTrackerController, RecallsController } from 'doughnut-api'
import makeMe from 'doughnut-test-fixtures/makeMe'
import type { MockInstance } from 'vitest'
import { baseNoteTimes } from './recallJustReviewInteractive.fixtures.js'

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
