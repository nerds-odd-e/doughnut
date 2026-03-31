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

const MEMORY_TRACKER_ID = 1
const SPELL_PROMPT_ID = 42

/** Wrong spelling still POSTs an answer; SRS rescheduling is server-side (RecallPromptControllerTests.WrongAnswer). */
async function waitForSpellingPromptVisible(
  lastFrame: () => string | undefined
): Promise<void> {
  await waitForLastFrame(
    lastFrame,
    (p) =>
      p.includes('Spell:') &&
      p.includes('Recalling') &&
      !p.includes('Loading spelling question')
  )
}

describe('recall spelling (interactive)', () => {
  let configDir: string
  let savedConfigDir: string | undefined
  let recallingSpy: ReturnType<typeof vi.spyOn>
  let showMemoryTrackerSpy: ReturnType<typeof vi.spyOn>
  let getRecallPromptsSpy: ReturnType<typeof vi.spyOn>
  let askAQuestionSpy: ReturnType<typeof vi.spyOn>
  let answerSpellingSpy: ReturnType<typeof vi.spyOn>

  function pendingSpellingPrompt(): RecallPrompt {
    return makeMe.aRecallPrompt
      .withId(SPELL_PROMPT_ID)
      .withSpellingStem('Spell the title')
      .withMemoryTrackerId(MEMORY_TRACKER_ID)
      .please()
  }

  function spellingDueList() {
    return makeMe.aDueMemoryTrackersList
      .totalAssimilatedCount(0)
      .toRepeat([{ memoryTrackerId: MEMORY_TRACKER_ID, spelling: true }])
      .please()
  }

  beforeEach(() => {
    configDir = tempConfigWithToken()
    savedConfigDir = process.env.DOUGHNUT_CONFIG_DIR
    process.env.DOUGHNUT_CONFIG_DIR = configDir

    const noteRealm = makeMe.aNoteRealm
      .title('sedition')
      .notebookTitle('NB')
      .details('body')
      .createdAt(baseNoteTimes.createdAt)
      .updatedAt(baseNoteTimes.updatedAt)
      .please()

    recallingSpy = vi.spyOn(RecallsController, 'recalling').mockResolvedValue({
      data: spellingDueList(),
    } as Awaited<ReturnType<typeof RecallsController.recalling>>)

    const tracker = {
      ...makeMe.aMemoryTracker
        .nextRecallAt('2026-06-01T00:00:00Z')
        .ofNote(noteRealm)
        .please(),
      id: MEMORY_TRACKER_ID,
      spelling: true as const,
    }
    showMemoryTrackerSpy = vi
      .spyOn(MemoryTrackerController, 'showMemoryTracker')
      .mockResolvedValue({
        data: tracker,
      } as Awaited<
        ReturnType<typeof MemoryTrackerController.showMemoryTracker>
      >)

    getRecallPromptsSpy = vi
      .spyOn(MemoryTrackerController, 'getRecallPrompts')
      .mockRejectedValue(
        new Error('unexpected getRecallPrompts in spelling path')
      )

    askAQuestionSpy = vi.spyOn(MemoryTrackerController, 'askAQuestion')
    askAQuestionSpy.mockResolvedValue({
      data: pendingSpellingPrompt(),
    } as Awaited<ReturnType<typeof MemoryTrackerController.askAQuestion>>)

    answerSpellingSpy = vi.spyOn(RecallPromptController, 'answerSpelling')
  })

  afterEach(() => {
    recallingSpy.mockRestore()
    showMemoryTrackerSpy.mockRestore()
    getRecallPromptsSpy.mockRestore()
    askAQuestionSpy.mockRestore()
    answerSpellingSpy.mockRestore()
    if (savedConfigDir === undefined) {
      delete process.env.DOUGHNUT_CONFIG_DIR
    } else {
      process.env.DOUGHNUT_CONFIG_DIR = savedConfigDir
    }
    fs.rmSync(configDir, { recursive: true, force: true })
  })

  test('wrong spelling shows Incorrect., records answer with correct false, no success lines', async () => {
    let recallingAfterWrong = 0
    recallingSpy.mockImplementation(() => {
      recallingAfterWrong += 1
      const empty = makeMe.aDueMemoryTrackersList
        .totalAssimilatedCount(0)
        .toRepeat([])
        .please()
      const data = recallingAfterWrong === 1 ? spellingDueList() : empty
      return Promise.resolve({
        data,
      } as Awaited<ReturnType<typeof RecallsController.recalling>>)
    })

    const pending = pendingSpellingPrompt()
    answerSpellingSpy.mockResolvedValue({
      data: {
        ...pending,
        answer: { correct: false, spellingAnswer: 'typo' },
      },
    } as Awaited<ReturnType<typeof RecallPromptController.answerSpelling>>)

    const { stdin, frames, lastFrame } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/recall\r')
    await waitForSpellingPromptVisible(lastFrame)

    stdin.write('typo\r')

    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (p) => p.includes('Incorrect.') && p.includes('Recalled 1 note')
    )

    const plain = stripAnsi(frames.join('\n'))
    expect(plain).not.toContain('Correct!')
    expect(plain).not.toContain('Recalled successfully')
    expect(plain).toContain('Recalled 1 note')

    expect(answerSpellingSpy).toHaveBeenCalledTimes(1)
    expect(answerSpellingSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { recallPrompt: SPELL_PROMPT_ID },
        body: { spellingAnswer: 'typo' },
      })
    )
  })

  test('mixed-case answer is sent as typed; server correct flag ends session (NoteTitle is case-insensitive on backend)', async () => {
    let recallingCalls = 0
    recallingSpy.mockImplementation(() => {
      recallingCalls += 1
      const data =
        recallingCalls === 1
          ? spellingDueList()
          : makeMe.aDueMemoryTrackersList
              .totalAssimilatedCount(0)
              .toRepeat([])
              .please()
      return Promise.resolve({
        data,
      } as Awaited<ReturnType<typeof RecallsController.recalling>>)
    })

    const pending = pendingSpellingPrompt()
    answerSpellingSpy.mockImplementation((opts) => {
      expect(opts.body.spellingAnswer).toBe('SeDiTiOn')
      return Promise.resolve({
        data: {
          ...pending,
          answer: { correct: true, spellingAnswer: 'SeDiTiOn' },
        },
      } as Awaited<ReturnType<typeof RecallPromptController.answerSpelling>>)
    })

    const { stdin, frames, lastFrame } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/recall\r')
    await waitForSpellingPromptVisible(lastFrame)

    stdin.write('SeDiTiOn\r')

    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (p) => p.includes('Spell correct: sedition')
    )

    expect(answerSpellingSpy).toHaveBeenCalledTimes(1)
  })

  test('submitted spelling trims NBSP and preserves mixed case in API body', async () => {
    let recallingCalls = 0
    recallingSpy.mockImplementation(() => {
      recallingCalls += 1
      const data =
        recallingCalls === 1
          ? spellingDueList()
          : makeMe.aDueMemoryTrackersList
              .totalAssimilatedCount(0)
              .toRepeat([])
              .please()
      return Promise.resolve({
        data,
      } as Awaited<ReturnType<typeof RecallsController.recalling>>)
    })

    const pending = pendingSpellingPrompt()
    answerSpellingSpy.mockResolvedValue({
      data: {
        ...pending,
        answer: { correct: true, spellingAnswer: 'SeDiTiOn' },
      },
    } as Awaited<ReturnType<typeof RecallPromptController.answerSpelling>>)

    const { stdin, frames, lastFrame } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/recall\r')
    await waitForSpellingPromptVisible(lastFrame)

    stdin.write('\u00A0SeDiTiOn\u00A0\r')

    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (p) => p.includes('Spell correct: sedition')
    )

    expect(answerSpellingSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        body: { spellingAnswer: 'SeDiTiOn' },
      })
    )
  })

  test('Esc from spelling shows leave recall confirmation without calling answerSpelling', async () => {
    const { stdin, frames, lastFrame } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/recall\r')
    await waitForSpellingPromptVisible(lastFrame)

    expect(answerSpellingSpy).not.toHaveBeenCalled()

    pressEscape(stdin)
    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (p) => p.includes(LEAVE_RECALL_PROMPT) && p.includes('(y/n)')
    )

    expect(answerSpellingSpy).not.toHaveBeenCalled()
  })

  test('after Esc from spelling, y settles with Recall session stopped and never calls answerSpelling', async () => {
    const { stdin, frames, lastFrame } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/recall\r')
    await waitForSpellingPromptVisible(lastFrame)
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

    expect(answerSpellingSpy).not.toHaveBeenCalled()
  })

  test('after Esc from spelling, n returns to spelling card without answerSpelling; buffer preserved', async () => {
    const { stdin, frames, lastFrame } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/recall\r')
    await waitForSpellingPromptVisible(lastFrame)

    stdin.write('par')
    await waitForLastFrame(lastFrame, (p) => p.includes('> par'))

    pressEscape(stdin)
    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (p) => p.includes(LEAVE_RECALL_PROMPT)
    )

    stdin.write('n\r')

    await waitForLastFrame(
      lastFrame,
      (p) =>
        p.includes('Spell:') &&
        p.includes('Spell the title') &&
        p.includes('> par') &&
        !p.includes(LEAVE_RECALL_PROMPT)
    )

    expect(answerSpellingSpy).not.toHaveBeenCalled()
  })

  test('empty Enter on spelling leave confirm stays on confirm; n returns with buffer', async () => {
    const { stdin, frames, lastFrame } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/recall\r')
    await waitForSpellingPromptVisible(lastFrame)
    stdin.write('par')
    await waitForLastFrame(lastFrame, (p) => p.includes('> par'))

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

    expect(answerSpellingSpy).not.toHaveBeenCalled()

    stdin.write('n\r')
    await waitForLastFrame(
      lastFrame,
      (p) =>
        p.includes('Spell:') &&
        p.includes('> par') &&
        !p.includes(LEAVE_RECALL_PROMPT)
    )

    expect(answerSpellingSpy).not.toHaveBeenCalled()
  })
})
