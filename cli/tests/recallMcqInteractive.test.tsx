import * as fs from 'node:fs'
import {
  MemoryTrackerController,
  RecallPromptController,
  RecallsController,
} from 'doughnut-api'
import type { RecallPrompt } from 'doughnut-api'
import makeMe from 'doughnut-test-fixtures/makeMe'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
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
const LEAVE_RECALL_CONFIRM = 'Leave recall?'
const RECALL_SESSION_STOPPED = 'Recall session stopped.'

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
      (p) => p.includes(LEAVE_RECALL_CONFIRM) && p.includes('(y/n)')
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
      (p) => p.includes(LEAVE_RECALL_CONFIRM)
    )

    stdin.write('y\r')

    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (p) => p.includes(RECALL_SESSION_STOPPED)
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
      (p) => p.includes(LEAVE_RECALL_CONFIRM)
    )

    stdin.write('n\r')

    await waitForLastFrame(
      lastFrame,
      (p) =>
        p.includes('Choose') &&
        p.includes('Alpha') &&
        p.includes(MCQ_HINT_SUBSTR) &&
        !p.includes(LEAVE_RECALL_CONFIRM)
    )

    expect(answerQuizSpy).not.toHaveBeenCalled()
  })
})
