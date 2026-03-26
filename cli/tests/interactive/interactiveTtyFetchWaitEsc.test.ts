import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  mockContestAndRegenerate,
  mockRecallNext,
  mockRecallStatus,
} from './interactiveRecallMockAccess.js'
import { userAbortError } from '../../src/fetchAbort.js'
import {
  isInRecallSubstate,
  resetRecallStateForTesting,
} from '../../src/interactive.js'
import { stripAnsi } from '../../src/renderer.js'
import {
  endTTYSession,
  pressKey,
  pushTTYCommandBytes,
  pushTTYCommandEnter,
  startTTYSessionWithoutRecallReset,
  submitTTYCommand,
  tick,
  ttyOutput,
  type TTYStdin,
} from './interactiveTestHelpers.js'
import { recallNextQuestion } from '../recallNextTestShapes.js'
import { mcqRecallPrompt } from '../recallPromptFixtures.js'

describe('TTY recall load wait — Esc cancels', () => {
  let writeSpy: ReturnType<typeof vi.spyOn>
  let stdin: TTYStdin

  beforeEach(async () => {
    resetRecallStateForTesting()
    mockRecallNext.mockReset()
    mockRecallNext.mockImplementation((_due, signal) => {
      return new Promise((_resolve, reject) => {
        signal?.addEventListener('abort', () => reject(userAbortError()), {
          once: true,
        })
      })
    })
    ;({ stdin, writeSpy } = await startTTYSessionWithoutRecallReset())
  })

  afterEach(() => {
    endTTYSession(stdin)
  })

  test('while recall fetch is in flight, prompt shows loading recall copy', async () => {
    await submitTTYCommand(stdin, '/recall')
    await tick()
    expect(stripAnsi(ttyOutput(writeSpy))).toContain('Loading recall questions')
  })

  test('Esc aborts recall fetch and shows Cancelled by user.', async () => {
    await submitTTYCommand(stdin, '/recall')
    await tick()
    pressKey(stdin, 'escape')
    await vi.waitFor(() =>
      expect(ttyOutput(writeSpy)).toContain('Cancelled by user.')
    )
    expect(isInRecallSubstate()).toBe(false)
  })
})

describe('TTY recall-status wait — Esc cancels', () => {
  let writeSpy: ReturnType<typeof vi.spyOn>
  let stdin: TTYStdin

  beforeEach(async () => {
    resetRecallStateForTesting()
    mockRecallStatus.mockReset()
    mockRecallStatus.mockImplementation((signal?: AbortSignal) => {
      return new Promise((_resolve, reject) => {
        signal?.addEventListener('abort', () => reject(userAbortError()), {
          once: true,
        })
      })
    })
    ;({ stdin, writeSpy } = await startTTYSessionWithoutRecallReset())
  })

  afterEach(async () => {
    endTTYSession(stdin)
    const actual = await vi.importActual<typeof import('../../src/recall.js')>(
      '../../src/recall.js'
    )
    mockRecallStatus.mockImplementation((signal?: AbortSignal) =>
      actual.recallStatus(signal)
    )
  })

  test('while recall-status fetch is in flight, prompt shows loading status copy', async () => {
    await submitTTYCommand(stdin, '/recall-status')
    await tick()
    expect(stripAnsi(ttyOutput(writeSpy))).toContain('Loading recall status')
  })

  test('Esc aborts recall-status fetch and shows Cancelled by user.', async () => {
    await submitTTYCommand(stdin, '/recall-status')
    await tick()
    pressKey(stdin, 'escape')
    await vi.waitFor(() =>
      expect(ttyOutput(writeSpy)).toContain('Cancelled by user.')
    )
  })
})

describe('TTY contest wait — Esc cancels', () => {
  let writeSpy: ReturnType<typeof vi.spyOn>
  let stdin: TTYStdin

  beforeEach(async () => {
    resetRecallStateForTesting()
    mockRecallNext.mockReset()
    mockContestAndRegenerate.mockReset()
    mockRecallNext.mockResolvedValue(
      recallNextQuestion(mcqRecallPrompt(100, 'Q?', ['A', 'B']))
    )
    mockContestAndRegenerate.mockImplementation((_id, signal?: AbortSignal) => {
      return new Promise((_resolve, reject) => {
        signal?.addEventListener('abort', () => reject(userAbortError()), {
          once: true,
        })
      })
    })
    ;({ stdin, writeSpy } = await startTTYSessionWithoutRecallReset())
  })

  afterEach(() => {
    endTTYSession(stdin)
  })

  test('Esc aborts contest fetch and shows Cancelled by user.', async () => {
    await submitTTYCommand(stdin, '/recall')
    pushTTYCommandBytes(stdin, '/contest ')
    await tick()
    pushTTYCommandEnter(stdin)
    await vi.waitFor(() =>
      expect(ttyOutput(writeSpy)).toContain('Regenerating question')
    )
    pressKey(stdin, 'escape')
    await vi.waitFor(() =>
      expect(ttyOutput(writeSpy)).toContain('Cancelled by user.')
    )
    expect(isInRecallSubstate()).toBe(true)
  })
})
