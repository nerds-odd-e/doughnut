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
import {
  deferred,
  waitBusySubmitAnswer,
  waitLoadingNextQuestion,
} from './recallInteractiveShared.js'
import { pressEscape, renderInkWhenCommandLineReady } from './inkTestHelpers.js'
import {
  leaveRecallWithYnRe,
  startRecall,
  waitMcqIncorrectOnLastFrame,
  waitMcqLoadMore,
  waitMcqVisible,
  waitReturnsToMcq,
} from './recallMcqInteractive.waits.js'
import { tempConfigWithToken } from './tempConfigTestHelpers.js'

const baseNoteTimes = {
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

const RECALL_PROMPT_ID = 42

const EXPECT_GUIDANCE_MORE_BELOW = '↓ more below'

function reLiteral(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

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
      .withId(pending.id)
      .withNote(mcqFixtureNoteRealm.note)
      .withPredefinedQuestion(pending.predefinedQuestion!)
      .withAnswer(answer)
      .withMemoryTrackerId(memoryTrackerId)
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
      .createdAt(baseNoteTimes.createdAt)
      .updatedAt(baseNoteTimes.updatedAt)
      .please()

    recallingSpy = vi.spyOn(RecallsController, 'recalling').mockResolvedValue({
      data: makeMe.aDueMemoryTrackersList
        .totalAssimilatedCount(0)
        .toRepeat([{ memoryTrackerId: 1, spelling: false }])
        .please(),
    } as Awaited<ReturnType<typeof RecallsController.recalling>>)

    showMemoryTrackerSpy = vi
      .spyOn(MemoryTrackerController, 'showMemoryTracker')
      .mockRejectedValue(new Error('unexpected showMemoryTracker in MCQ path'))

    getRecallPromptsSpy = vi
      .spyOn(MemoryTrackerController, 'getRecallPrompts')
      .mockResolvedValue({
        data: [pendingMcqPrompt()],
      } as Awaited<ReturnType<typeof MemoryTrackerController.getRecallPrompts>>)

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

  test('many MCQ choices use a fixed-height list with more-below', async () => {
    const manyChoicesCount = 15
    getRecallPromptsSpy.mockResolvedValue({
      data: [
        makeMe.aRecallPrompt
          .withId(RECALL_PROMPT_ID)
          .withQuestionStem('Pick one')
          .withChoices(
            Array.from({ length: manyChoicesCount }, (_, i) => `c${i}`)
          )
          .please(),
      ],
    } as Awaited<ReturnType<typeof MemoryTrackerController.getRecallPrompts>>)

    const ink = await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(ink.stdin)
    await ink.waitUntilLastFrame(
      (p) =>
        p.includes('Pick one') &&
        p.includes(EXPECT_GUIDANCE_MORE_BELOW) &&
        p.includes('1. c0')
    )

    const plain = ink.lastStrippedFrame()
    expect(plain).not.toMatch(
      new RegExp(`${manyChoicesCount}\\.\\s*c${manyChoicesCount - 1}`)
    )
  })

  test('wrong MCQ choice shows Incorrect and sends 0-based choiceIndex to API', async () => {
    mockSingleMcqDue()
    const pending = pendingMcqPrompt()
    answerQuizSpy.mockResolvedValue({
      data: mcqAnsweredPrompt(pending, {
        id: 100,
        correct: false,
        choiceIndex: 1,
      }),
    } as Awaited<ReturnType<typeof RecallPromptController.answerQuiz>>)

    const ink = await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(ink.stdin)
    await waitMcqVisible(ink)
    ink.stdin.write('2\r')
    await waitMcqIncorrectOnLastFrame(ink)
    await waitMcqLoadMore(ink)

    expect(ink.lastStrippedFrame()).toContain('Beta')
    expect(answerQuizSpy).toHaveBeenCalledTimes(1)
    expect(answerQuizSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { recallPrompt: RECALL_PROMPT_ID },
        body: { choiceIndex: 1 },
      })
    )
  })

  test('shows busy label in bordered input while answerQuiz is pending', async () => {
    mockSingleMcqDue()
    const pending = pendingMcqPrompt()
    const { promise: answerPromise, resolve: resolveAnswer } =
      deferred<Awaited<ReturnType<typeof RecallPromptController.answerQuiz>>>()
    answerQuizSpy.mockImplementation(() => answerPromise)

    const ink = await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(ink.stdin)
    await waitMcqVisible(ink)
    ink.stdin.write('2\r')
    await waitBusySubmitAnswer(ink)

    resolveAnswer({
      data: mcqAnsweredPrompt(pending, {
        id: 100,
        correct: false,
        choiceIndex: 1,
      }),
    } as Awaited<ReturnType<typeof RecallPromptController.answerQuiz>>)

    await waitMcqLoadMore(ink)
  })

  test('after first MCQ answer, shows loading next label until second tracker loads', async () => {
    const SECOND_PROMPT_ID = 99
    const secondStem = 'SECOND_MCQ_LOADING_NEXT_UNIQUE'
    const pending = pendingMcqPrompt()
    const secondPrompt = makeMe.aRecallPrompt
      .withId(SECOND_PROMPT_ID)
      .withQuestionStem(secondStem)
      .withChoices(['X', 'Y', 'Z'])
      .please()

    recallingSpy.mockResolvedValue({
      data: makeMe.aDueMemoryTrackersList
        .totalAssimilatedCount(0)
        .toRepeat([
          { memoryTrackerId: 1, spelling: false as const },
          { memoryTrackerId: 2, spelling: false as const },
        ])
        .please(),
    } as Awaited<ReturnType<typeof RecallsController.recalling>>)

    let resolvePrompts2!: (
      value: Awaited<
        ReturnType<typeof MemoryTrackerController.getRecallPrompts>
      >
    ) => void
    const prompts2Promise = new Promise<
      Awaited<ReturnType<typeof MemoryTrackerController.getRecallPrompts>>
    >((resolve) => {
      resolvePrompts2 = resolve
    })

    getRecallPromptsSpy.mockImplementation((opts) => {
      const id = opts.path.memoryTracker
      if (id === 1) {
        return Promise.resolve({
          data: [pending],
        } as Awaited<
          ReturnType<typeof MemoryTrackerController.getRecallPrompts>
        >)
      }
      if (id === 2) {
        return prompts2Promise
      }
      throw new Error(`unexpected memoryTracker ${String(id)}`)
    })

    answerQuizSpy.mockResolvedValue({
      data: mcqAnsweredPrompt(pending, {
        id: 100,
        correct: false,
        choiceIndex: 1,
      }),
    } as Awaited<ReturnType<typeof RecallPromptController.answerQuiz>>)

    const ink = await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(ink.stdin)
    await waitMcqVisible(ink)
    ink.stdin.write('2\r')

    await waitLoadingNextQuestion(ink)

    resolvePrompts2({
      data: [secondPrompt],
    } as Awaited<ReturnType<typeof MemoryTrackerController.getRecallPrompts>>)

    await ink.waitForLastFrameToInclude(secondStem)
  })

  test('out-of-range MCQ number does not call answerQuiz; valid answer still works', async () => {
    mockSingleMcqDue()
    const pending = pendingMcqPrompt()
    answerQuizSpy.mockResolvedValue({
      data: mcqAnsweredPrompt(pending, {
        id: 100,
        correct: false,
        choiceIndex: 1,
      }),
    } as Awaited<ReturnType<typeof RecallPromptController.answerQuiz>>)

    const ink = await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(ink.stdin)
    await waitMcqVisible(ink)
    ink.stdin.write('9\r')
    await ink.waitForLastFrameToInclude('→ 9')
    expect(answerQuizSpy).not.toHaveBeenCalled()

    ink.stdin.write('\x7f2\r')
    await waitMcqIncorrectOnLastFrame(ink)
    expect(answerQuizSpy).toHaveBeenCalledTimes(1)
  })

  test('wrong MCQ then next due tracker shows second question without ending recall', async () => {
    const SECOND_PROMPT_ID = 99
    const secondStem = 'SECOND_MCQ_STEM_UNIQUE'
    const pending = pendingMcqPrompt()
    const secondPrompt = makeMe.aRecallPrompt
      .withId(SECOND_PROMPT_ID)
      .withQuestionStem(secondStem)
      .withChoices(['X', 'Y', 'Z'])
      .please()

    const note2 = makeMe.aNoteRealm
      .title('Beta')
      .content('body2')
      .createdAt(baseNoteTimes.createdAt)
      .updatedAt(baseNoteTimes.updatedAt)
      .please()

    recallingSpy.mockResolvedValue({
      data: makeMe.aDueMemoryTrackersList
        .totalAssimilatedCount(0)
        .toRepeat([
          { memoryTrackerId: 1, spelling: false as const },
          { memoryTrackerId: 2, spelling: false as const },
        ])
        .please(),
    } as Awaited<ReturnType<typeof RecallsController.recalling>>)

    getRecallPromptsSpy.mockImplementation((opts) => {
      const id = opts.path.memoryTracker
      if (id === 1) {
        return Promise.resolve({
          data: [pending],
        } as Awaited<
          ReturnType<typeof MemoryTrackerController.getRecallPrompts>
        >)
      }
      if (id === 2) {
        return Promise.resolve({
          data: [secondPrompt],
        } as Awaited<
          ReturnType<typeof MemoryTrackerController.getRecallPrompts>
        >)
      }
      throw new Error(`unexpected memoryTracker ${String(id)}`)
    })

    let answerN = 0
    answerQuizSpy.mockImplementation(() => {
      answerN += 1
      if (answerN === 1) {
        return Promise.resolve({
          data: mcqAnsweredPrompt(pending, {
            id: 100,
            correct: false,
            choiceIndex: 1,
          }),
        } as Awaited<ReturnType<typeof RecallPromptController.answerQuiz>>)
      }
      return Promise.resolve({
        data: makeMe.anAnsweredQuestion
          .withId(secondPrompt.id)
          .withNote(note2.note)
          .withPredefinedQuestion(secondPrompt.predefinedQuestion!)
          .withAnswer({ id: 101, correct: true, choiceIndex: 0 })
          .withMemoryTrackerId(2)
          .please(),
      } as Awaited<ReturnType<typeof RecallPromptController.answerQuiz>>)
    })

    const ink = await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(ink.stdin)
    await waitMcqVisible(ink)
    ink.stdin.write('2\r')
    await ink.waitForLastFrameToInclude(
      new RegExp(`(?=.*Incorrect)(?=.*${reLiteral(secondStem)})`, 's')
    )
    expect(answerQuizSpy).toHaveBeenCalledTimes(1)

    ink.stdin.write('1\r')
    await ink.waitForLastFrameToInclude('Correct!')
    expect(ink.lastStrippedFrame()).toContain(secondStem)
    expect(answerQuizSpy).toHaveBeenCalledTimes(2)
    expect(recallingSpy).toHaveBeenCalledTimes(1)
  })

  test('after Esc, y settles with Recall session stopped and never calls answerQuiz', async () => {
    const ink = await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(ink.stdin)
    await waitMcqVisible(ink)
    await pressEscape(ink.stdin)
    await ink.waitForLastFrameToInclude(leaveRecallWithYnRe)
    ink.stdin.write('y\r')
    await ink.waitForLastFrameToInclude('Recall session stopped.')
    expect(answerQuizSpy).not.toHaveBeenCalled()
  })

  test('after Esc, n returns to MCQ without answerQuiz; buffer preserved', async () => {
    const ink = await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(ink.stdin)
    await waitMcqVisible(ink)
    ink.stdin.write('z')
    await ink.waitUntilLastFrame((p) => p.includes('→ z'))
    await pressEscape(ink.stdin)
    await ink.waitForLastFrameToInclude(/Leave recall\?/)
    ink.stdin.write('n\r')
    await ink.waitUntilLastFrame(
      (p) =>
        p.includes('→ z') &&
        p.includes('Choose') &&
        !p.includes('Leave recall?')
    )
    expect(answerQuizSpy).not.toHaveBeenCalled()
  })

  test('after Esc then n, MCQ list highlight preserved (Enter submits second choice)', async () => {
    mockSingleMcqDue()
    const pending = pendingMcqPrompt()
    answerQuizSpy.mockResolvedValue({
      data: mcqAnsweredPrompt(pending, {
        id: 100,
        correct: false,
        choiceIndex: 1,
      }),
    } as Awaited<ReturnType<typeof RecallPromptController.answerQuiz>>)

    const ink = await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(ink.stdin)
    await waitMcqVisible(ink)
    ink.stdin.write('\u001b[B')
    await ink.waitUntilLastFrame((p) => p.includes('2.'))
    await pressEscape(ink.stdin)
    await ink.waitForLastFrameToInclude(/Leave recall\?/)
    ink.stdin.write('n\r')
    await waitReturnsToMcq(ink)
    ink.stdin.write('\r')
    await waitMcqIncorrectOnLastFrame(ink)
    expect(answerQuizSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { recallPrompt: RECALL_PROMPT_ID },
        body: { choiceIndex: 1 },
      })
    )
  })

  test('empty Enter on leave recall confirm stays on confirm; n returns to MCQ', async () => {
    const ink = await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(ink.stdin)
    await waitMcqVisible(ink)
    await pressEscape(ink.stdin)
    await ink.waitForLastFrameToInclude(/Leave recall\?/)
    ink.stdin.write('\r')
    await ink.waitForLastFrameToInclude(/Leave recall\?/)
    ink.stdin.write('n\r')
    await waitReturnsToMcq(ink)
    expect(answerQuizSpy).not.toHaveBeenCalled()
  })

  test('rejected contest shows advice in session strip and does not call regenerate', async () => {
    const rejectAdvice = 'UNIQUE_REJECT_ADVICE_9_3'
    contestSpy = vi.spyOn(RecallPromptController, 'contest').mockResolvedValue({
      data: { rejected: true, advice: rejectAdvice },
    } as Awaited<ReturnType<typeof RecallPromptController.contest>>)
    regenerateSpy = vi
      .spyOn(RecallPromptController, 'regenerate')
      .mockResolvedValue({
        data: pendingMcqQuestion(),
      } as Awaited<ReturnType<typeof RecallPromptController.regenerate>>)

    const ink = await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(ink.stdin)
    await waitMcqVisible(ink)
    ink.stdin.write('/contest\r')
    await ink.waitForLastFrameToInclude(rejectAdvice)
    expect(ink.lastStrippedFrame()).toContain('Choose')
    expect(regenerateSpy).not.toHaveBeenCalled()
    expect(answerQuizSpy).not.toHaveBeenCalled()
  })

  test('contest API error settles with user-visible message and leaves recall', async () => {
    contestSpy = vi
      .spyOn(RecallPromptController, 'contest')
      .mockRejectedValue(new Error('contest failed hard'))

    const ink = await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(ink.stdin)
    await waitMcqVisible(ink)
    ink.stdin.write('/contest\r')
    await ink.waitForLastFrameToInclude('Doughnut service is not available')
    expect(answerQuizSpy).not.toHaveBeenCalled()
  })
})
