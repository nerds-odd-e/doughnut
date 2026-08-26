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
  waitLoadingSpellingNext,
} from './recallInteractiveShared.js'
import {
  waitReturnsToSpellingWithBuffer,
  waitSpellingCorrect,
  waitSpellingIncorrect,
  waitSpellingPromptVisible,
} from './recallSpellingInteractive.waits.js'
import { tempConfigWithToken } from './tempConfigTestHelpers.js'

export const MEMORY_TRACKER_ID = 1
export const SPELL_PROMPT_ID = 42
export const SPELL_PROMPT_ID_2 = 43
export const SPELL_PLACEHOLDER_SUBSTR = 'Type answer, Enter to submit'

export type RecallSpellingInteractiveApi = {
  test: typeof test
  expect: typeof expect
  makeMe: typeof makeMe
  MemoryTrackerController: typeof MemoryTrackerController
  RecallPromptController: typeof RecallPromptController
  RecallsController: typeof RecallsController
  InteractiveCliApp: typeof InteractiveCliApp
  SPELL_PROMPT_ID: typeof SPELL_PROMPT_ID
  SPELL_PROMPT_ID_2: typeof SPELL_PROMPT_ID_2
  SPELL_PLACEHOLDER_SUBSTR: typeof SPELL_PLACEHOLDER_SUBSTR
  leaveRecallWithYnRe: typeof leaveRecallWithYnRe
  deferred: typeof deferred
  startRecall: typeof startRecall
  waitBusySubmitAnswer: typeof waitBusySubmitAnswer
  waitLoadingSpellingNext: typeof waitLoadingSpellingNext
  renderInkWhenCommandLineReady: typeof renderInkWhenCommandLineReady
  pressEscape: typeof pressEscape
  waitSpellingPromptVisible: typeof waitSpellingPromptVisible
  waitSpellingIncorrect: typeof waitSpellingIncorrect
  waitSpellingCorrect: typeof waitSpellingCorrect
  waitReturnsToSpellingWithBuffer: typeof waitReturnsToSpellingWithBuffer
  recallingSpy: ReturnType<typeof vi.spyOn>
  getRecallPromptSpy: ReturnType<typeof vi.spyOn>
  answerSpellingSpy: ReturnType<typeof vi.spyOn>
  pendingSpellingPrompt: () => RecallPrompt
  spellingAnsweredPrompt: (
    pending: RecallPrompt,
    answer: { correct: boolean; spellingAnswer: string }
  ) => AnsweredQuestion
  mockRecallingFirstThenEmpty: () => void
}

/** Wrong spelling still POSTs an answer; SRS rescheduling is server-side. */
export function describeRecallSpellingInteractive(
  register: (api: RecallSpellingInteractiveApi) => void
): void {
  describe('recall spelling (interactive)', () => {
    let configDir: string
    let savedConfigDir: string | undefined
    let recallingSpy: ReturnType<typeof vi.spyOn>
    let showMemoryTrackerSpy: ReturnType<typeof vi.spyOn>
    let getRecallPromptsSpy: ReturnType<typeof vi.spyOn>
    let getRecallPromptSpy: ReturnType<typeof vi.spyOn>
    let answerSpellingSpy: ReturnType<typeof vi.spyOn>
    let spellingFixtureNoteRealm: NoteRealm

    function pendingSpellingPrompt(): RecallPrompt {
      return makeMe.aRecallPrompt
        .withId(SPELL_PROMPT_ID)
        .withSpellingStem('Spell the title')
        .please()
    }

    function spellingDueList() {
      return makeMe.aDueMemoryTrackersList
        .totalAssimilatedCount(0)
        .toRepeat([{ memoryTrackerId: MEMORY_TRACKER_ID, spelling: true }])
        .please()
    }

    function spellingAnsweredPrompt(
      pending: RecallPrompt,
      answer: { correct: boolean; spellingAnswer: string }
    ): AnsweredQuestion {
      return makeMe.anAnsweredQuestion
        .withId(pending.id)
        .withNote(spellingFixtureNoteRealm.note)
        .withAnswer({
          id: 1,
          correct: answer.correct,
          spellingAnswer: answer.spellingAnswer,
        })
        .spelling()
        .withMemoryTrackerId(MEMORY_TRACKER_ID)
        .please()
    }

    function mockRecallingFirstThenEmpty() {
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
    }

    beforeEach(() => {
      configDir = tempConfigWithToken()
      savedConfigDir = process.env.DOUGHNUT_CONFIG_DIR
      process.env.DOUGHNUT_CONFIG_DIR = configDir

      spellingFixtureNoteRealm = makeMe.aNoteRealm
        .title('sedition')
        .content('body')
        .please()

      recallingSpy = vi
        .spyOn(RecallsController, 'recalling')
        .mockResolvedValue({
          data: spellingDueList(),
        } as Awaited<ReturnType<typeof RecallsController.recalling>>)

      showMemoryTrackerSpy = vi
        .spyOn(MemoryTrackerController, 'showMemoryTracker')
        .mockRejectedValue(
          new Error('unexpected showMemoryTracker in spelling path')
        )

      getRecallPromptsSpy = vi
        .spyOn(MemoryTrackerController, 'getRecallPrompts')
        .mockRejectedValue(
          new Error('unexpected getRecallPrompts in spelling path')
        )

      getRecallPromptSpy = vi.spyOn(MemoryTrackerController, 'getRecallPrompt')
      getRecallPromptSpy.mockResolvedValue({
        data: pendingSpellingPrompt(),
      } as Awaited<ReturnType<typeof MemoryTrackerController.getRecallPrompt>>)

      answerSpellingSpy = vi.spyOn(RecallPromptController, 'answerSpelling')
    })

    afterEach(() => {
      recallingSpy.mockRestore()
      showMemoryTrackerSpy.mockRestore()
      getRecallPromptsSpy.mockRestore()
      getRecallPromptSpy.mockRestore()
      answerSpellingSpy.mockRestore()
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
      makeMe,
      MemoryTrackerController,
      RecallPromptController,
      RecallsController,
      InteractiveCliApp,
      SPELL_PROMPT_ID,
      SPELL_PROMPT_ID_2,
      SPELL_PLACEHOLDER_SUBSTR,
      leaveRecallWithYnRe,
      deferred,
      startRecall,
      waitBusySubmitAnswer,
      waitLoadingSpellingNext,
      renderInkWhenCommandLineReady,
      pressEscape,
      waitSpellingPromptVisible,
      waitSpellingIncorrect,
      waitSpellingCorrect,
      waitReturnsToSpellingWithBuffer,
      get recallingSpy() {
        return recallingSpy
      },
      get getRecallPromptSpy() {
        return getRecallPromptSpy
      },
      get answerSpellingSpy() {
        return answerSpellingSpy
      },
      pendingSpellingPrompt,
      spellingAnsweredPrompt,
      mockRecallingFirstThenEmpty,
    })
  })
}
