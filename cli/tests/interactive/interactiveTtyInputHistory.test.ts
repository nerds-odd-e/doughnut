import './interactiveTestMocks.js'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import {
  isInRecallSubstate,
  resetRecallStateForTesting,
} from '../../src/interactive.js'
import { stripAnsi } from '../../src/renderer.js'
import { recallNextQuestion } from '../recallNextTestShapes.js'
import { mcqRecallPrompt } from '../recallPromptFixtures.js'
import {
  mockAnswerQuiz,
  mockRecallNext,
} from './interactiveRecallMockAccess.js'
import {
  endTTYSession,
  lastStdoutLineContaining,
  pushTTYCommandBytes,
  pushTTYCommandEnter,
  pushTTYCommandKey,
  startTTYSessionWithoutRecallReset,
  submitTTYCommand,
  tick,
  ttyOutput,
  ttySessionWithSpies,
  type TTYStdin,
} from './interactiveTestHelpers.js'

describe('TTY: user input history (↑↓)', () => {
  describe('recalling a prior line from in-memory history', () => {
    let writeSpy: ReturnType<typeof vi.spyOn>
    let stdin: TTYStdin

    beforeEach(async () => {
      ;({ stdin, writeSpy } = await ttySessionWithSpies())
    })

    afterEach(async () => {
      await endTTYSession(stdin)
    })

    test('shows the previous submitted line in the input box after ↑↑ with a new draft', async () => {
      const marker = 'history-recall-xyz'
      pushTTYCommandBytes(stdin, marker)
      pushTTYCommandEnter(stdin)
      await tick()
      await tick()

      pushTTYCommandBytes(stdin, 'draft')
      await tick()
      pushTTYCommandKey(stdin, 'up')
      await tick()
      pushTTYCommandKey(stdin, 'up')
      await tick()

      expect(stripAnsi(ttyOutput(writeSpy))).toContain(`→ ${marker}`)
    })
  })

  describe('recall MCQ submit is not in user input history', () => {
    let writeSpy: ReturnType<typeof vi.spyOn>
    let stdin: TTYStdin

    beforeEach(async () => {
      resetRecallStateForTesting()
      mockRecallNext
        .mockReset()
        .mockResolvedValueOnce(
          recallNextQuestion(
            mcqRecallPrompt(100, 'Pick one', ['Alpha', 'Beta'])
          )
        )
        .mockResolvedValue({
          type: 'none',
          message: '0 notes to recall today',
        })
      mockAnswerQuiz.mockReset()
      mockAnswerQuiz.mockResolvedValue({ correct: true })
      ;({ stdin, writeSpy } = await startTTYSessionWithoutRecallReset())
    })

    afterEach(async () => {
      await endTTYSession(stdin)
    })

    test('after recall session ends, ↑ recalls /recall not the MCQ choice line', async () => {
      await submitTTYCommand(stdin, '/help')
      await tick()
      await tick()

      await submitTTYCommand(stdin, '/recall')
      await tick()
      await tick()

      pushTTYCommandEnter(stdin)
      await tick()
      await new Promise((r) => setTimeout(r, 50))

      expect(mockAnswerQuiz).toHaveBeenCalledWith(100, 0, expect.any(Number))
      expect(stripAnsi(ttyOutput(writeSpy))).toContain('Recalled successfully')

      pushTTYCommandBytes(stdin, 'n')
      await tick()
      await tick()

      await vi.waitFor(() => {
        expect(isInRecallSubstate()).toBe(false)
      })

      pushTTYCommandKey(stdin, 'up')
      await tick()
      await tick()

      const out = stripAnsi(ttyOutput(writeSpy))
      const arrowLine = lastStdoutLineContaining(out, '→')
      expect(arrowLine).toBeDefined()
      expect(arrowLine).toContain('/recall')
      expect(arrowLine.trimEnd()).not.toMatch(/→\s*1\s*$/)
    })
  })
})
