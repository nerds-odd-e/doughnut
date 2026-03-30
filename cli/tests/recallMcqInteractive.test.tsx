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
    getRecallPromptsSpy.mockResolvedValue({
      data: [
        makeMe.aRecallPrompt
          .withId(RECALL_PROMPT_ID)
          .withQuestionStem('Pick one')
          .withChoices(Array.from({ length: 8 }, (_, i) => `c${i}`))
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
    expect(plain).not.toMatch(/8\.\s*c7/)
  })

  test('wrong MCQ choice shows Incorrect and sends 0-based choiceIndex to API', async () => {
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
})
