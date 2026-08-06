import * as fs from 'node:fs'
import {
  MemoryTrackerController,
  RecallPromptController,
  RecallsController,
} from 'doughnut-api'
import type {
  AnsweredQuestion,
  NoteRealm,
  RecallPromptHistoryItem,
  RecallQuestion,
} from 'doughnut-api'
import makeMe from 'doughnut-test-fixtures/makeMe'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { InteractiveCliApp } from '../src/InteractiveCliApp.js'
import { pressEscape, renderInkWhenCommandLineReady } from './inkTestHelpers.js'
import {
  deferred,
  leaveRecallWithYnRe,
  startRecall,
  waitBusySubmitAnswer,
  waitLoadingNextQuestion,
} from './recallInteractiveShared.js'
import {
  waitMcqIncorrectOnLastFrame,
  waitMcqLoadMore,
  waitMcqVisible,
  waitReturnsToMcq,
} from './recallMcqInteractive.waits.js'
import { tempConfigWithToken } from './tempConfigTestHelpers.js'

export const RECALL_PROMPT_ID = 42
export const EXPECT_GUIDANCE_MORE_BELOW = '↓ more below'

export type RecallMcqInteractiveApi = {
  test: typeof test
  expect: typeof expect
  vi: typeof vi
  makeMe: typeof makeMe
  MemoryTrackerController: typeof MemoryTrackerController
  RecallPromptController: typeof RecallPromptController
  RecallsController: typeof RecallsController
  InteractiveCliApp: typeof InteractiveCliApp
  RECALL_PROMPT_ID: typeof RECALL_PROMPT_ID
  EXPECT_GUIDANCE_MORE_BELOW: typeof EXPECT_GUIDANCE_MORE_BELOW
  leaveRecallWithYnRe: typeof leaveRecallWithYnRe
  deferred: typeof deferred
  startRecall: typeof startRecall
  waitBusySubmitAnswer: typeof waitBusySubmitAnswer
  waitLoadingNextQuestion: typeof waitLoadingNextQuestion
  renderInkWhenCommandLineReady: typeof renderInkWhenCommandLineReady
  pressEscape: typeof pressEscape
  waitMcqVisible: typeof waitMcqVisible
  waitMcqLoadMore: typeof waitMcqLoadMore
  waitMcqIncorrectOnLastFrame: typeof waitMcqIncorrectOnLastFrame
  waitReturnsToMcq: typeof waitReturnsToMcq
  recallingSpy: ReturnType<typeof vi.spyOn>
  getRecallPromptsSpy: ReturnType<typeof vi.spyOn>
  answerQuizSpy: ReturnType<typeof vi.spyOn>
  pendingMcqPrompt: () => RecallPromptHistoryItem
  pendingMcqQuestion: (
    id?: number,
    stem?: string,
    choices?: string[]
  ) => RecallQuestion
  mcqAnsweredPrompt: (
    pending: RecallPromptHistoryItem,
    answer: { id: number; correct: boolean; choiceIndex: number },
    memoryTrackerId?: number
  ) => AnsweredQuestion
  mockSingleMcqDue: () => void
  setContestSpy: (spy: ReturnType<typeof vi.spyOn>) => void
  setRegenerateSpy: (spy: ReturnType<typeof vi.spyOn>) => void
}

export function describeRecallMcqInteractive(
  register: (api: RecallMcqInteractiveApi) => void
): void {
  describe('recall MCQ (interactive)', () => {
    let configDir: string
    let savedConfigDir: string | undefined
    let recallingSpy: ReturnType<typeof vi.spyOn>
    let showMemoryTrackerSpy: ReturnType<typeof vi.spyOn>
    let getRecallPromptsSpy: ReturnType<typeof vi.spyOn>
    let askAQuestionSpy: ReturnType<typeof vi.spyOn>
    let answerQuizSpy: ReturnType<typeof vi.spyOn>
    let contestSpy: ReturnType<typeof vi.spyOn> | undefined
    let regenerateSpy: ReturnType<typeof vi.spyOn> | undefined
    let mcqFixtureNoteRealm: NoteRealm

    function pendingMcqPrompt(): RecallPromptHistoryItem {
      return makeMe.aRecallPrompt
        .withId(RECALL_PROMPT_ID)
        .withQuestionStem('Choose **Alpha**?')
        .withChoices(['First', '**Beta**', 'Third'])
        .please()
    }

    function pendingMcqQuestion(
      id = RECALL_PROMPT_ID,
      stem = 'Choose **Alpha**?',
      choices: string[] = ['First', '**Beta**', 'Third']
    ): RecallQuestion {
      return makeMe.aRecallQuestion
        .withId(id)
        .withQuestionStem(stem)
        .withChoices(choices)
        .please()
    }

    function mcqAnsweredPrompt(
      pending: RecallPromptHistoryItem,
      answer: { id: number; correct: boolean; choiceIndex: number },
      memoryTrackerId = 1
    ): AnsweredQuestion {
      return makeMe.anAnsweredQuestion
        .fromMcqHistoryItem(pending, mcqFixtureNoteRealm.note, memoryTrackerId)
        .withAnswer(answer)
        .please()
    }

    function mockSingleMcqDue() {
      recallingSpy.mockResolvedValue({
        data: makeMe.aDueMemoryTrackersList
          .totalAssimilatedCount(0)
          .toRepeat([{ memoryTrackerId: 1, spelling: false }])
          .please(),
      } as Awaited<ReturnType<typeof RecallsController.recalling>>)
    }

    beforeEach(() => {
      configDir = tempConfigWithToken()
      savedConfigDir = process.env.DOUGHNUT_CONFIG_DIR
      process.env.DOUGHNUT_CONFIG_DIR = configDir

      mcqFixtureNoteRealm = makeMe.aNoteRealm
        .title('Alpha')
        .content('body')
        .please()

      recallingSpy = vi
        .spyOn(RecallsController, 'recalling')
        .mockResolvedValue({
          data: makeMe.aDueMemoryTrackersList
            .totalAssimilatedCount(0)
            .toRepeat([{ memoryTrackerId: 1, spelling: false }])
            .please(),
        } as Awaited<ReturnType<typeof RecallsController.recalling>>)

      showMemoryTrackerSpy = vi
        .spyOn(MemoryTrackerController, 'showMemoryTracker')
        .mockRejectedValue(
          new Error('unexpected showMemoryTracker in MCQ path')
        )

      getRecallPromptsSpy = vi
        .spyOn(MemoryTrackerController, 'getRecallPrompts')
        .mockResolvedValue({
          data: [pendingMcqPrompt()],
        } as Awaited<
          ReturnType<typeof MemoryTrackerController.getRecallPrompts>
        >)

      askAQuestionSpy = vi
        .spyOn(MemoryTrackerController, 'askAQuestion')
        .mockRejectedValue(new Error('stub: MCQ from getRecallPrompts'))

      answerQuizSpy = vi.spyOn(RecallPromptController, 'answerQuiz')
    })

    afterEach(() => {
      contestSpy?.mockRestore()
      contestSpy = undefined
      regenerateSpy?.mockRestore()
      regenerateSpy = undefined
      recallingSpy.mockRestore()
      showMemoryTrackerSpy.mockRestore()
      getRecallPromptsSpy.mockRestore()
      askAQuestionSpy.mockRestore()
      answerQuizSpy.mockRestore()
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
      RecallPromptController,
      RecallsController,
      InteractiveCliApp,
      RECALL_PROMPT_ID,
      EXPECT_GUIDANCE_MORE_BELOW,
      leaveRecallWithYnRe,
      deferred,
      startRecall,
      waitBusySubmitAnswer,
      waitLoadingNextQuestion,
      renderInkWhenCommandLineReady,
      pressEscape,
      waitMcqVisible,
      waitMcqLoadMore,
      waitMcqIncorrectOnLastFrame,
      waitReturnsToMcq,
      get recallingSpy() {
        return recallingSpy
      },
      get getRecallPromptsSpy() {
        return getRecallPromptsSpy
      },
      get answerQuizSpy() {
        return answerQuizSpy
      },
      pendingMcqPrompt,
      pendingMcqQuestion,
      mcqAnsweredPrompt,
      mockSingleMcqDue,
      setContestSpy: (spy) => {
        contestSpy = spy
      },
      setRegenerateSpy: (spy) => {
        regenerateSpy = spy
      },
    })
  })
}
