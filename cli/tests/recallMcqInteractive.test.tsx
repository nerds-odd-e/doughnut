import * as fs from 'node:fs'
import {
  MemoryTrackerController,
  RecallPromptController,
  RecallsController,
} from 'doughnut-api'
import type { RecallPrompt } from 'doughnut-api'
import makeMe from 'doughnut-test-fixtures/makeMe'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import {
  LEAVE_RECALL_PROMPT,
  RECALL_SESSION_STOPPED_LINE,
} from '../src/commands/recall/leaveRecallSessionCopy.js'
import { InteractiveCliApp } from '../src/InteractiveCliApp.js'
import {
  pressEscape,
  renderInkWhenCommandLineReady,
  stripAnsi,
  waitForFrames,
  waitForLastFrame,
} from './inkTestHelpers.js'
import { tempConfigWithToken } from './tempConfigTestHelpers.js'

const baseNoteTimes = {
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

const RECALL_PROMPT_ID = 42

const MCQ_HINT_SUBSTR = '↑↓ Enter or number to select'

const EXPECT_GUIDANCE_MORE_BELOW = '↓ more below'

async function waitForMcqVisible(frames: string[]): Promise<void> {
  await waitForFrames(
    () => stripAnsi(frames.join('\n')),
    (p) =>
      p.includes('Choose') &&
      p.includes('Alpha') &&
      !p.includes('**') &&
      p.includes('Beta') &&
      p.includes(MCQ_HINT_SUBSTR)
  )
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

  function pendingMcqPrompt(): RecallPrompt {
    return makeMe.aRecallPrompt
      .withId(RECALL_PROMPT_ID)
      .withQuestionStem('Choose **Alpha**?')
      .withChoices(['First', '**Beta**', 'Third'])
      .withMemoryTrackerId(1)
      .please()
  }

  beforeEach(() => {
    configDir = tempConfigWithToken()
    savedConfigDir = process.env.DOUGHNUT_CONFIG_DIR
    process.env.DOUGHNUT_CONFIG_DIR = configDir

    const noteRealm = makeMe.aNoteRealm
      .title('Alpha')
      .notebookTitle('NB')
      .details('body')
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
      .mockResolvedValue({
        data: makeMe.aMemoryTracker
          .nextRecallAt('2026-06-01T00:00:00Z')
          .ofNote(noteRealm)
          .please(),
      } as Awaited<
        ReturnType<typeof MemoryTrackerController.showMemoryTracker>
      >)

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
          .withMemoryTrackerId(1)
          .please(),
      ],
    } as Awaited<ReturnType<typeof MemoryTrackerController.getRecallPrompts>>)

    const { stdin, frames, lastFrame } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/recall\r')

    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (p) =>
        p.includes('Pick one') &&
        p.includes(EXPECT_GUIDANCE_MORE_BELOW) &&
        p.includes('1. c0')
    )

    const plain = stripAnsi(lastFrame() ?? '')
    expect(plain).toContain(EXPECT_GUIDANCE_MORE_BELOW)
    expect(plain).toMatch(/1\.\s*c0/)
    expect(plain).not.toMatch(
      new RegExp(`${manyChoicesCount}\\.\\s*c${manyChoicesCount - 1}`)
    )
  })

  test('wrong MCQ choice shows Incorrect and sends 0-based choiceIndex to API', async () => {
    let recallingAfterWrong = 0
    recallingSpy.mockImplementation(() => {
      recallingAfterWrong += 1
      const empty = makeMe.aDueMemoryTrackersList
        .totalAssimilatedCount(0)
        .toRepeat([])
        .please()
      const data =
        recallingAfterWrong === 1
          ? makeMe.aDueMemoryTrackersList
              .totalAssimilatedCount(0)
              .toRepeat([{ memoryTrackerId: 1, spelling: false }])
              .please()
          : empty
      return Promise.resolve({
        data,
      } as Awaited<ReturnType<typeof RecallsController.recalling>>)
    })

    const pending = pendingMcqPrompt()
    answerQuizSpy.mockResolvedValue({
      data: {
        ...pending,
        answer: { id: 100, correct: false, choiceIndex: 1 },
      },
    } as Awaited<ReturnType<typeof RecallPromptController.answerQuiz>>)

    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/recall\r')

    await waitForMcqVisible(frames)

    expect(frames.join('\n')).toContain('\u001b[')

    stdin.write('2\r')

    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (p) => p.includes('Load more from next 3 days?')
    )

    const plainWrong = stripAnsi(frames.join('\n'))
    expect(plainWrong).toContain('Alpha')
    expect(plainWrong).toContain('Choose')
    expect(plainWrong).toContain('First')
    expect(plainWrong).toContain('Beta')
    expect(plainWrong).toContain('Incorrect.')

    const rawWrong = frames.join('\n')
    const incorrectIdx = rawWrong.indexOf('Incorrect.')
    expect(incorrectIdx).toBeGreaterThan(-1)
    const beforeIncorrect = rawWrong.slice(0, incorrectIdx)
    expect(
      beforeIncorrect.includes('\u001b[31m') ||
        beforeIncorrect.includes('\u001b[91m')
    ).toBe(true)
    expect(
      beforeIncorrect.includes('\u001b[32m') ||
        beforeIncorrect.includes('\u001b[92m')
    ).toBe(true)

    expect(answerQuizSpy).toHaveBeenCalledTimes(1)
    expect(answerQuizSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { recallPrompt: RECALL_PROMPT_ID },
        body: { choiceIndex: 1 },
      })
    )
  })

  test('out-of-range MCQ number does not call answerQuiz; valid answer still works', async () => {
    let recallingAfterWrong = 0
    recallingSpy.mockImplementation(() => {
      recallingAfterWrong += 1
      const empty = makeMe.aDueMemoryTrackersList
        .totalAssimilatedCount(0)
        .toRepeat([])
        .please()
      const data =
        recallingAfterWrong === 1
          ? makeMe.aDueMemoryTrackersList
              .totalAssimilatedCount(0)
              .toRepeat([{ memoryTrackerId: 1, spelling: false }])
              .please()
          : empty
      return Promise.resolve({
        data,
      } as Awaited<ReturnType<typeof RecallsController.recalling>>)
    })

    const pending = pendingMcqPrompt()
    answerQuizSpy.mockResolvedValue({
      data: {
        ...pending,
        answer: { id: 100, correct: false, choiceIndex: 1 },
      },
    } as Awaited<ReturnType<typeof RecallPromptController.answerQuiz>>)

    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/recall\r')
    await waitForMcqVisible(frames)

    stdin.write('9\r')
    await waitForFrames(
      () => stripAnsi(frames.at(-1) ?? ''),
      (p) => p.includes('→ 9')
    )

    expect(answerQuizSpy).not.toHaveBeenCalled()
    expect(stripAnsi(frames.join('\n'))).not.toContain('Incorrect.')

    stdin.write('\x7f')
    await waitForFrames(
      () => stripAnsi(frames.at(-1) ?? ''),
      (p) => p.includes('→') && !p.includes('→ 9')
    )
    stdin.write('2\r')

    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (p) => p.includes('Load more from next 3 days?')
    )

    expect(answerQuizSpy).toHaveBeenCalledTimes(1)
    expect(stripAnsi(frames.join('\n'))).toContain('Incorrect.')
  })

  test('wrong MCQ then next due tracker shows second question without ending recall', async () => {
    const SECOND_PROMPT_ID = 99
    const secondStem = 'SECOND_MCQ_STEM_UNIQUE'
    const pending = pendingMcqPrompt()
    const secondPrompt = makeMe.aRecallPrompt
      .withId(SECOND_PROMPT_ID)
      .withQuestionStem(secondStem)
      .withChoices(['X', 'Y', 'Z'])
      .withMemoryTrackerId(2)
      .please()

    const note1 = makeMe.aNoteRealm
      .title('Alpha')
      .notebookTitle('NB')
      .details('body')
      .createdAt(baseNoteTimes.createdAt)
      .updatedAt(baseNoteTimes.updatedAt)
      .please()
    const note2 = makeMe.aNoteRealm
      .title('Beta')
      .notebookTitle('NB')
      .details('body2')
      .createdAt(baseNoteTimes.createdAt)
      .updatedAt(baseNoteTimes.updatedAt)
      .please()

    let recallingN = 0
    recallingSpy.mockImplementation(() => {
      recallingN += 1
      const empty = makeMe.aDueMemoryTrackersList
        .totalAssimilatedCount(0)
        .toRepeat([])
        .please()
      const row1 = { memoryTrackerId: 1, spelling: false as const }
      const row2 = { memoryTrackerId: 2, spelling: false as const }
      const data =
        recallingN === 1
          ? makeMe.aDueMemoryTrackersList
              .totalAssimilatedCount(0)
              .toRepeat([row1])
              .please()
          : recallingN === 2
            ? makeMe.aDueMemoryTrackersList
                .totalAssimilatedCount(0)
                .toRepeat([row2])
                .please()
            : empty
      return Promise.resolve({
        data,
      } as Awaited<ReturnType<typeof RecallsController.recalling>>)
    })

    showMemoryTrackerSpy.mockImplementation((opts) => {
      const id = opts.path.memoryTracker
      if (id === 1) {
        return Promise.resolve({
          data: makeMe.aMemoryTracker
            .nextRecallAt('2026-06-01T00:00:00Z')
            .ofNote(note1)
            .please(),
        } as Awaited<
          ReturnType<typeof MemoryTrackerController.showMemoryTracker>
        >)
      }
      if (id === 2) {
        return Promise.resolve({
          data: makeMe.aMemoryTracker
            .nextRecallAt('2026-06-01T00:00:00Z')
            .ofNote(note2)
            .please(),
        } as Awaited<
          ReturnType<typeof MemoryTrackerController.showMemoryTracker>
        >)
      }
      throw new Error(`unexpected memoryTracker ${String(id)}`)
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
          data: {
            ...pending,
            answer: { id: 100, correct: false, choiceIndex: 1 },
          },
        } as Awaited<ReturnType<typeof RecallPromptController.answerQuiz>>)
      }
      return Promise.resolve({
        data: {
          ...secondPrompt,
          answer: { id: 101, correct: true, choiceIndex: 0 },
        },
      } as Awaited<ReturnType<typeof RecallPromptController.answerQuiz>>)
    })

    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/recall\r')
    await waitForMcqVisible(frames)
    stdin.write('2\r')

    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (p) => p.includes('Incorrect') && p.includes(secondStem)
    )

    const plainFirstWrong = stripAnsi(frames.join('\n'))
    expect(plainFirstWrong).toContain('Alpha')
    expect(plainFirstWrong).toContain('Choose')
    expect(plainFirstWrong).toContain('First')
    expect(plainFirstWrong).toContain('Beta')

    expect(answerQuizSpy).toHaveBeenCalledTimes(1)

    stdin.write('1\r')
    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (p) => p.includes('Correct!')
    )

    const plainCorrect = stripAnsi(frames.join('\n'))
    expect(plainCorrect).toContain('Beta')
    expect(plainCorrect).toContain(secondStem)
    expect(plainCorrect).toContain('X')
    expect(plainCorrect).toContain('Correct!')

    const rawCorrect = frames.join('\n')
    const correctIdx = rawCorrect.lastIndexOf('Correct!')
    expect(correctIdx).toBeGreaterThan(-1)
    const beforeCorrect = rawCorrect.slice(0, correctIdx)
    expect(
      beforeCorrect.includes('\u001b[32m') ||
        beforeCorrect.includes('\u001b[92m')
    ).toBe(true)

    expect(answerQuizSpy).toHaveBeenCalledTimes(2)
  })

  test('Esc from MCQ shows leave recall confirmation without calling answerQuiz', async () => {
    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/recall\r')
    await waitForMcqVisible(frames)

    expect(answerQuizSpy).not.toHaveBeenCalled()

    pressEscape(stdin)
    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (p) => p.includes(LEAVE_RECALL_PROMPT) && p.includes('(y/n)')
    )

    expect(answerQuizSpy).not.toHaveBeenCalled()
  })

  test('after Esc, y settles with Recall session stopped and never calls answerQuiz', async () => {
    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/recall\r')
    await waitForMcqVisible(frames)
    pressEscape(stdin)
    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (p) => p.includes(LEAVE_RECALL_PROMPT)
    )

    stdin.write('y\r')

    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (p) => p.includes(RECALL_SESSION_STOPPED_LINE)
    )

    expect(answerQuizSpy).not.toHaveBeenCalled()
  })

  test('after Esc, n returns to MCQ without answerQuiz', async () => {
    const { stdin, frames, lastFrame } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/recall\r')
    await waitForMcqVisible(frames)
    pressEscape(stdin)
    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (p) => p.includes(LEAVE_RECALL_PROMPT)
    )

    stdin.write('n\r')

    await waitForLastFrame(
      lastFrame,
      (p) =>
        p.includes('Choose') &&
        p.includes('Alpha') &&
        p.includes(MCQ_HINT_SUBSTR) &&
        !p.includes(LEAVE_RECALL_PROMPT)
    )

    expect(answerQuizSpy).not.toHaveBeenCalled()
  })

  test('after Esc then n, MCQ list highlight preserved (Enter submits second choice)', async () => {
    const pending = pendingMcqPrompt()
    answerQuizSpy.mockResolvedValue({
      data: {
        ...pending,
        answer: { id: 100, correct: false, choiceIndex: 1 },
      },
    } as Awaited<ReturnType<typeof RecallPromptController.answerQuiz>>)

    const { stdin, frames, lastFrame } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/recall\r')
    await waitForMcqVisible(frames)
    stdin.write('\u001b[B')
    await waitForLastFrame(lastFrame, (p) => stripAnsi(p).includes('2.'))

    pressEscape(stdin)
    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (p) => p.includes(LEAVE_RECALL_PROMPT)
    )

    stdin.write('n\r')
    await waitForLastFrame(
      lastFrame,
      (p) => p.includes(MCQ_HINT_SUBSTR) && !p.includes(LEAVE_RECALL_PROMPT)
    )

    stdin.write('\r')

    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (p) => p.includes('Incorrect')
    )

    expect(answerQuizSpy).toHaveBeenCalledTimes(1)
    expect(answerQuizSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { recallPrompt: RECALL_PROMPT_ID },
        body: { choiceIndex: 1 },
      })
    )
  })

  test('after Esc then n, MCQ command buffer preserved', async () => {
    const { stdin, frames, lastFrame } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/recall\r')
    await waitForMcqVisible(frames)
    stdin.write('z')
    await waitForLastFrame(lastFrame, (p) => p.includes('→ z'))

    pressEscape(stdin)
    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (p) => p.includes(LEAVE_RECALL_PROMPT)
    )

    stdin.write('n\r')

    await waitForLastFrame(
      lastFrame,
      (p) =>
        p.includes('→ z') &&
        p.includes('Choose') &&
        !p.includes(LEAVE_RECALL_PROMPT)
    )

    expect(answerQuizSpy).not.toHaveBeenCalled()
  })

  test('empty Enter on leave recall confirm stays on confirm; n returns to MCQ', async () => {
    const { stdin, frames, lastFrame } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/recall\r')
    await waitForMcqVisible(frames)
    pressEscape(stdin)
    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (p) => p.includes(LEAVE_RECALL_PROMPT)
    )

    stdin.write('\r')
    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (p) => p.includes(LEAVE_RECALL_PROMPT)
    )

    expect(answerQuizSpy).not.toHaveBeenCalled()

    stdin.write('n\r')
    await waitForLastFrame(
      lastFrame,
      (p) =>
        p.includes('Choose') &&
        p.includes(MCQ_HINT_SUBSTR) &&
        !p.includes(LEAVE_RECALL_PROMPT)
    )

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
        data: pendingMcqPrompt(),
      } as Awaited<ReturnType<typeof RecallPromptController.regenerate>>)

    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/recall\r')
    await waitForMcqVisible(frames)

    stdin.write('/contest\r')

    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (p) => p.includes(rejectAdvice)
    )

    const plain = stripAnsi(frames.join('\n'))
    expect(plain).toContain(rejectAdvice)
    expect(plain).toContain('Choose')
    expect(plain).toContain(MCQ_HINT_SUBSTR)
    expect(regenerateSpy).not.toHaveBeenCalled()
    expect(answerQuizSpy).not.toHaveBeenCalled()
  })

  test('contest API error settles with user-visible message and leaves recall', async () => {
    contestSpy = vi
      .spyOn(RecallPromptController, 'contest')
      .mockRejectedValue(new Error('contest failed hard'))

    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/recall\r')
    await waitForMcqVisible(frames)

    stdin.write('/contest\r')

    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (p) => p.includes('Doughnut service is not available')
    )

    expect(answerQuizSpy).not.toHaveBeenCalled()
  })
})
