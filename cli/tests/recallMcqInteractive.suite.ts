import * as fs from 'node:fs'
import {
  MemoryTrackerController,
  RecallPromptController,
  RecallsController,
} from 'donut-api'
import type { AnsweredQuestion, NoteRealm, RecallPrompt } from 'donut-api'
import makeMe from 'donut-test-fixtures/makeMe'
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
  RecallPromptController: typeof RecallPromptController
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
  setMcqQuestion: (
    trackerId: number,
    prompt: RecallPrompt | Promise<RecallPrompt>
  ) => void
  answerSpy: ReturnType<typeof vi.spyOn>
  pendingMcqQuestion: (
    id?: number,
    stem?: string,
    choices?: string[]
  ) => RecallPrompt
  mcqAnsweredPrompt: (
    pending: RecallPrompt,
    answer: { id: number; correct: boolean; choiceIndex: number },
    memoryTrackerId?: number
  ) => AnsweredQuestion
  setDueMcqTrackers: (...trackerIds: number[]) => void
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
    let getRecallPromptSpy: ReturnType<typeof vi.spyOn>
    let answerSpy: ReturnType<typeof vi.spyOn>
    let contestSpy: ReturnType<typeof vi.spyOn> | undefined
    let regenerateSpy: ReturnType<typeof vi.spyOn> | undefined
    let mcqFixtureNoteRealm: NoteRealm

    const questions = new Map<number, RecallPrompt | Promise<RecallPrompt>>()

    async function questionFor(trackerId: number) {
      const question = questions.get(trackerId)
      if (!question) throw new Error(`unexpected memoryTracker ${trackerId}`)
      return question
    }

    function pendingMcqQuestion(
      id = RECALL_PROMPT_ID,
      stem = 'Choose **Alpha**?',
      choices: string[] = ['First', '**Beta**', 'Third']
    ): RecallPrompt {
      return makeMe.aRecallPrompt
        .withId(id)
        .withQuestionStem(stem)
        .withChoices(choices)
        .please()
    }

    function mcqAnsweredPrompt(
      pending: RecallPrompt,
      answer: { id: number; correct: boolean; choiceIndex: number },
      memoryTrackerId = 1
    ): AnsweredQuestion {
      return makeMe.anAnsweredQuestion
        .fromMcqPrompt(pending, mcqFixtureNoteRealm.note, memoryTrackerId)
        .withAnswer(answer)
        .please()
    }

    function setDueMcqTrackers(...trackerIds: number[]) {
      recallingSpy.mockResolvedValue({
        data: makeMe.aDueMemoryTrackersList
          .totalAssimilatedCount(0)
          .toRepeat(
            trackerIds.map((memoryTrackerId) => ({
              memoryTrackerId,
              spelling: false,
            }))
          )
          .please(),
      } as Awaited<ReturnType<typeof RecallsController.recalling>>)
    }

    beforeEach(() => {
      configDir = tempConfigWithToken()
      savedConfigDir = process.env.DONUT_CONFIG_DIR
      process.env.DONUT_CONFIG_DIR = configDir

      mcqFixtureNoteRealm = makeMe.aNoteRealm
        .title('Alpha')
        .content('body')
        .please()

      recallingSpy = vi.spyOn(RecallsController, 'recalling')
      setDueMcqTrackers(1)
      questions.clear()
      questions.set(1, pendingMcqQuestion())

      showMemoryTrackerSpy = vi
        .spyOn(MemoryTrackerController, 'showMemoryTracker')
        .mockRejectedValue(
          new Error('unexpected showMemoryTracker in MCQ path')
        )

      getRecallPromptSpy = vi
        .spyOn(MemoryTrackerController, 'getRecallPrompt')
        .mockImplementation(
          async (options) =>
            ({
              data: await questionFor(options.path.memoryTracker),
            }) as Awaited<
              ReturnType<typeof MemoryTrackerController.getRecallPrompt>
            >
        )

      answerSpy = vi.spyOn(RecallPromptController, 'answer')
    })

    afterEach(() => {
      contestSpy?.mockRestore()
      contestSpy = undefined
      regenerateSpy?.mockRestore()
      regenerateSpy = undefined
      recallingSpy.mockRestore()
      showMemoryTrackerSpy.mockRestore()
      getRecallPromptSpy.mockRestore()
      answerSpy.mockRestore()
      if (savedConfigDir === undefined) {
        delete process.env.DONUT_CONFIG_DIR
      } else {
        process.env.DONUT_CONFIG_DIR = savedConfigDir
      }
      fs.rmSync(configDir, { recursive: true, force: true })
    })

    register({
      test,
      expect,
      vi,
      makeMe,
      RecallPromptController,
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
      setMcqQuestion: (trackerId, question) => {
        questions.set(trackerId, question)
      },
      get answerSpy() {
        return answerSpy
      },
      pendingMcqQuestion,
      mcqAnsweredPrompt,
      setDueMcqTrackers,
      setContestSpy: (spy) => {
        contestSpy = spy
      },
      setRegenerateSpy: (spy) => {
        regenerateSpy = spy
      },
    })
  })
}
