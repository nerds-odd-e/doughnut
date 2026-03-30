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
  renderInkWhenCommandLineReady,
  stripAnsi,
  waitForFrames,
} from './inkTestHelpers.js'
import { tempConfigWithToken } from './tempConfigTestHelpers.js'

const baseNoteTimes = {
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

const RECALL_PROMPT_ID = 42

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
      .withQuestionStem('Stem question?')
      .withChoices(['First', 'Second', 'Third'])
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

    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (p) =>
        p.includes('Stem question?') &&
        p.includes('2. Second') &&
        p.includes('↑↓ Enter or number to select')
    )

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
})
