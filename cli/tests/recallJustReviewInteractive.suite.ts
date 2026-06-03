import * as fs from 'node:fs'
import { MemoryTrackerController, RecallsController } from 'doughnut-api'
import makeMe from 'doughnut-test-fixtures/makeMe'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import {
  LEAVE_RECALL_PROMPT,
  RECALL_SESSION_STOPPED_LINE,
} from '../src/commands/recall/leaveRecallSessionCopy.js'
import { RECALL_LOADING_NEXT_QUESTION_LABEL } from '../src/commands/recall/recallBusyInputCopy.js'
import { InteractiveCliApp } from '../src/InteractiveCliApp.js'
import {
  pressEscape,
  pressEscapeAndWaitForCancelledLine,
  renderInkWhenCommandLineReady,
  stripAnsi,
  waitForFrames,
  waitForLastFrame,
} from './inkTestHelpers.js'
import { tempConfigWithToken } from './tempConfigTestHelpers.js'
import {
  alphaNoteRealm,
  baseNoteTimes,
  childNoteUnderEnglish,
  leaveRecallWithYnRe,
} from './recallJustReviewInteractive.fixtures.js'
import {
  mockAlphaBetaNoteCards,
  mockMarkAsRecalledCounting,
  mockShowMemoryTrackerCardForRealm,
  setupTwoDueJustReviewItemsMocks,
} from './recallJustReviewInteractive.mocks.js'
import {
  emptyEnterAndInvalidLineStayOnRemember,
  recallSingleAlphaToLoadMore,
  startRecall,
  waitLoadMore,
  waitRecalledSummary,
  waitRememberCard,
  waitReturnsToSingleRememberCard,
} from './recallJustReviewInteractive.waits.js'

export {
  baseNoteTimes,
  leaveRecallWithYnRe,
} from './recallJustReviewInteractive.fixtures.js'
export type { InkWaitHelpers } from './recallJustReviewInteractive.waits.js'

export type RecallJustReviewInteractiveApi = {
  test: typeof test
  expect: typeof expect
  vi: typeof vi
  makeMe: typeof makeMe
  MemoryTrackerController: typeof MemoryTrackerController
  RecallsController: typeof RecallsController
  InteractiveCliApp: typeof InteractiveCliApp
  LEAVE_RECALL_PROMPT: typeof LEAVE_RECALL_PROMPT
  RECALL_SESSION_STOPPED_LINE: typeof RECALL_SESSION_STOPPED_LINE
  RECALL_LOADING_NEXT_QUESTION_LABEL: typeof RECALL_LOADING_NEXT_QUESTION_LABEL
  leaveRecallWithYnRe: typeof leaveRecallWithYnRe
  baseNoteTimes: typeof baseNoteTimes
  renderInkWhenCommandLineReady: typeof renderInkWhenCommandLineReady
  pressEscape: typeof pressEscape
  pressEscapeAndWaitForCancelledLine: typeof pressEscapeAndWaitForCancelledLine
  stripAnsi: typeof stripAnsi
  waitForFrames: typeof waitForFrames
  waitForLastFrame: typeof waitForLastFrame
  recallingSpy: ReturnType<typeof vi.spyOn>
  showMemoryTrackerSpy: ReturnType<typeof vi.spyOn>
  markAsRecalledSpy: ReturnType<typeof vi.spyOn>
  waitRememberCard: typeof waitRememberCard
  waitLoadMore: typeof waitLoadMore
  waitRecalledSummary: typeof waitRecalledSummary
  waitReturnsToSingleRememberCard: typeof waitReturnsToSingleRememberCard
  emptyEnterAndInvalidLineStayOnRemember: typeof emptyEnterAndInvalidLineStayOnRemember
  recallSingleAlphaToLoadMore: typeof recallSingleAlphaToLoadMore
  startRecall: typeof startRecall
  alphaNoteRealm: typeof alphaNoteRealm
  childNoteUnderEnglish: typeof childNoteUnderEnglish
  mockShowMemoryTrackerCardForRealm: (
    noteRealm: ReturnType<typeof alphaNoteRealm>
  ) => void
  mockMarkAsRecalledCounting: () => { n: number }
  setupTwoDueJustReviewItemsMocks: () => void
  mockAlphaBetaNoteCards: () => void
}

export function describeRecallJustReviewInteractive(
  register: (api: RecallJustReviewInteractiveApi) => void
): void {
  describe('recall just-review (interactive)', () => {
    let configDir: string
    let savedConfigDir: string | undefined
    let recallingSpy: ReturnType<typeof vi.spyOn>
    let showMemoryTrackerSpy: ReturnType<typeof vi.spyOn>
    let getRecallPromptsSpy: ReturnType<typeof vi.spyOn>
    let askAQuestionSpy: ReturnType<typeof vi.spyOn>
    let markAsRecalledSpy: ReturnType<typeof vi.spyOn>

    const spies = () => ({
      recallingSpy,
      showMemoryTrackerSpy,
      markAsRecalledSpy,
    })

    beforeEach(() => {
      configDir = tempConfigWithToken()
      savedConfigDir = process.env.DOUGHNUT_CONFIG_DIR
      process.env.DOUGHNUT_CONFIG_DIR = configDir

      let recallingCall = 0
      recallingSpy = vi
        .spyOn(RecallsController, 'recalling')
        .mockImplementation(async () => {
          recallingCall += 1
          const trackers =
            recallingCall === 1 ? [{ memoryTrackerId: 1, spelling: false }] : []
          return {
            data: makeMe.aDueMemoryTrackersList
              .totalAssimilatedCount(0)
              .toRepeat(trackers)
              .please(),
          } as Awaited<ReturnType<typeof RecallsController.recalling>>
        })

      showMemoryTrackerSpy = vi.spyOn(
        MemoryTrackerController,
        'showMemoryTracker'
      )

      getRecallPromptsSpy = vi
        .spyOn(MemoryTrackerController, 'getRecallPrompts')
        .mockResolvedValue({
          data: [],
        } as Awaited<
          ReturnType<typeof MemoryTrackerController.getRecallPrompts>
        >)

      askAQuestionSpy = vi
        .spyOn(MemoryTrackerController, 'askAQuestion')
        .mockRejectedValue(new Error('stub: no quiz in just-review tests'))

      markAsRecalledSpy = vi
        .spyOn(MemoryTrackerController, 'markAsRecalled')
        .mockResolvedValue({
          data: makeMe.aMemoryTracker.please(),
        } as Awaited<ReturnType<typeof MemoryTrackerController.markAsRecalled>>)
    })

    afterEach(() => {
      recallingSpy.mockRestore()
      showMemoryTrackerSpy.mockRestore()
      getRecallPromptsSpy.mockRestore()
      askAQuestionSpy.mockRestore()
      markAsRecalledSpy.mockRestore()
      if (savedConfigDir === undefined) {
        delete process.env.DOUGHNUT_CONFIG_DIR
      } else {
        process.env.DOUGHNUT_CONFIG_DIR = savedConfigDir
      }
      fs.rmSync(configDir, { recursive: true, force: true })
    })

    register({
      test,
      expect,
      vi,
      makeMe,
      MemoryTrackerController,
      RecallsController,
      InteractiveCliApp,
      LEAVE_RECALL_PROMPT,
      RECALL_SESSION_STOPPED_LINE,
      RECALL_LOADING_NEXT_QUESTION_LABEL,
      leaveRecallWithYnRe,
      baseNoteTimes,
      renderInkWhenCommandLineReady,
      pressEscape,
      pressEscapeAndWaitForCancelledLine,
      stripAnsi,
      waitForFrames,
      waitForLastFrame,
      get recallingSpy() {
        return recallingSpy
      },
      get showMemoryTrackerSpy() {
        return showMemoryTrackerSpy
      },
      get markAsRecalledSpy() {
        return markAsRecalledSpy
      },
      waitRememberCard,
      waitLoadMore,
      waitRecalledSummary,
      waitReturnsToSingleRememberCard,
      emptyEnterAndInvalidLineStayOnRemember,
      recallSingleAlphaToLoadMore,
      startRecall,
      alphaNoteRealm,
      childNoteUnderEnglish,
      mockShowMemoryTrackerCardForRealm: (noteRealm) =>
        mockShowMemoryTrackerCardForRealm(spies(), noteRealm),
      mockMarkAsRecalledCounting: () => mockMarkAsRecalledCounting(spies()),
      setupTwoDueJustReviewItemsMocks: () =>
        setupTwoDueJustReviewItemsMocks(spies()),
      mockAlphaBetaNoteCards: () => mockAlphaBetaNoteCards(spies()),
    })
  })
}
