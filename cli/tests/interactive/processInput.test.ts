import { describe, test, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  mockAnswerQuiz,
  mockAnswerSpelling,
  mockContestAndRegenerate,
  mockMarkAsRecalled,
  mockRecallNext,
} from './interactiveRecallMockAccess.js'
import { mkdtempSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { userAbortError } from '../../src/fetchAbort.js'
import {
  isInRecallSubstate,
  processInput,
  resetRecallStateForTesting,
} from '../../src/interactive.js'
import { stripAnsi } from '../../src/renderer.js'
import { makeTempConfigDir, withConfigDir } from './interactiveTestHelpers.js'

describe('processInput', () => {
  let logSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    resetRecallStateForTesting()
    mockRecallNext.mockClear()
    mockMarkAsRecalled.mockClear()
    mockAnswerQuiz.mockClear()
    mockAnswerSpelling.mockClear()
    mockContestAndRegenerate.mockClear()
    logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
  })

  afterEach(() => {
    vi.useRealTimers()
    logSpy.mockRestore()
  })

  // contract: default console (same observable surface as -c / piped log; not TTY bytes — see interactiveTty*.test.ts)

  test('returns true for exit commands', async () => {
    expect(await processInput('exit')).toBe(true)
    expect(await processInput('  exit  ')).toBe(true)
    expect(await processInput('/exit')).toBe(true)
    expect(await processInput('  /exit  ')).toBe(true)
  })

  test('returns false and does not log for empty input', async () => {
    expect(await processInput('')).toBe(false)
    expect(await processInput('   ')).toBe(false)
    expect(logSpy).not.toHaveBeenCalled()
  })

  test('returns false and logs "Not supported" for any other input', async () => {
    expect(await processInput('hello')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Not supported')
  })

  test('returns false and logs help for /help', async () => {
    expect(await processInput('/help')).toBe(false)
    const output = logSpy.mock.calls.flat().join('\n')
    expect(output).toContain('/add gmail')
    expect(output).toContain('/last email')
    expect(output).toContain('exit')
    expect(output).not.toContain('Not supported')
  })

  test('returns false and writes clear-screen sequence and prompt box for /clear', async () => {
    const writeSpy = vi
      .spyOn(process.stdout, 'write')
      .mockImplementation(() => true)
    expect(await processInput('/clear')).toBe(false)
    const output = writeSpy.mock.calls.map((c) => c[0]).join('')
    expect(output).toContain('\x1b[H\x1b[2J')
    expect(output).toContain('doughnut')
    expect(output).toContain('┌')
    expect(output).toContain('┘')
    expect(output).toContain('→')
    writeSpy.mockRestore()
  })

  test('returns false and shows usage for /add-access-token without token', async () => {
    expect(await processInput('/add-access-token')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Usage: /add-access-token <token>')
    logSpy.mockClear()
    expect(await processInput('/add-access-token ')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Usage: /add-access-token <token>')
  })

  test('returns false and shows tokens with default marker for /list-access-token', async () => {
    const configDir = makeTempConfigDir([
      { label: 'Token A', token: 'a' },
      { label: 'Token B', token: 'b' },
    ])
    const restore = withConfigDir(configDir)
    expect(await processInput('/list-access-token')).toBe(false)
    const output = logSpy.mock.calls.flat().join('\n')
    expect(output).toContain('★ Token A')
    expect(output).toContain('  Token B')
    restore()
  })

  test('returns false and removes token for /remove-access-token with label', async () => {
    const configDir = makeTempConfigDir([
      { label: 'Token A', token: 'a' },
      { label: 'Token B', token: 'b' },
    ])
    const restore = withConfigDir(configDir)
    expect(await processInput('/remove-access-token Token A')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Token "Token A" removed.')
    restore()
  })

  test('returns false and shows not found for /remove-access-token with unknown label', async () => {
    expect(await processInput('/remove-access-token Unknown')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Token "Unknown" not found.')
  })

  test.each([
    ['/remove-access-token ', 'Usage: /remove-access-token <label>'],
    [
      '/remove-access-token-completely ',
      'Usage: /remove-access-token-completely <label>',
    ],
    ['/create-access-token ', 'Usage: /create-access-token <label>'],
  ] as const)('returns false and shows usage for %s', async (input, expected) => {
    expect(await processInput(input)).toBe(false)
    expect(logSpy).toHaveBeenCalledWith(expected)
  })

  test('returns false and shows error for /create-access-token with no default token', async () => {
    const configDir = makeTempConfigDir([])
    const restore = withConfigDir(configDir)
    expect(await processInput('/create-access-token My New Token')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith(
      'No default access token. Add one first with /add-access-token.'
    )
    restore()
  })

  test('returns false and logs error for /last email when no account configured', async () => {
    const configDir = mkdtempSync(`${tmpdir()}/doughnut-test-`)
    const restore = withConfigDir(configDir)
    expect(await processInput('/last email')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith(
      'No Gmail account configured. Run /add gmail first.'
    )
    restore()
  })

  // contract: /recall session (mock recallNext + default console). recall.test.ts covers recall.ts API and /recall load cancel with real recallNext.

  test('spelling: prompt with markdown stem shows ANSI codes, not raw markdown', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'spelling',
      recallPromptId: 100,
      stem: '**bold** and *italic*',
    })

    await processInput('/recall')
    const spellCall = logSpy.mock.calls.find(
      (c) => typeof c[0] === 'string' && c[0].includes('Spell:')
    )
    expect(spellCall).toBeDefined()
    const output = spellCall![0] as string
    expect(output).toContain('\x1b[')
    expect(output).not.toContain('**')
    expect(output).not.toContain('*italic*')
    expect(stripAnsi(output)).toContain('bold')
    expect(stripAnsi(output)).toContain('italic')
  })

  test('spelling: Spell prompt, answer calls answerSpelling with thinkingTimeMs, then success messages', async () => {
    vi.useFakeTimers()
    mockRecallNext.mockResolvedValue({
      type: 'spelling',
      recallPromptId: 100,
      stem: 'means incite violence',
    })
    mockAnswerSpelling.mockResolvedValue({ correct: true })

    await processInput('/recall')
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Spell: means incite violence')
    )
    logSpy.mockClear()

    vi.advanceTimersByTime(5000)
    await processInput('sedition')
    expect(mockAnswerSpelling).toHaveBeenCalledWith(100, 'sedition', 5000)
    expect(logSpy).toHaveBeenCalledWith('Correct!')
    expect(logSpy).toHaveBeenCalledWith('Recalled successfully')
  })

  test('spelling: empty input prompts to type', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'spelling',
      recallPromptId: 100,
      stem: '...',
    })
    mockAnswerSpelling.mockResolvedValue({ correct: false })

    await processInput('/recall')
    logSpy.mockClear()
    expect(await processInput('')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Please type your spelling')
    )
    await processInput('clear') // clear pending state for subsequent tests
  })

  test('/recall with no notes prompts load more from next 3 days', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'none',
      message: '0 notes to recall today',
    })

    await processInput('/recall')
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Load more from next 3 days? (y/n)')
    )
    expect(mockRecallNext).toHaveBeenCalledWith(0, expect.any(AbortSignal))
  })

  test('/recall load more: user says n, shows 0 notes to recall today', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'none',
      message: '0 notes to recall today',
    })

    await processInput('/recall')
    logSpy.mockClear()
    await processInput('n')
    expect(logSpy).toHaveBeenCalledWith('0 notes to recall today')
    expect(mockRecallNext).toHaveBeenCalledTimes(1)
  })

  test('/recall load more: user says y, fetches with dueindays 3 and continues', async () => {
    mockRecallNext
      .mockResolvedValueOnce({
        type: 'none',
        message: '0 notes to recall today',
      })
      .mockResolvedValueOnce({
        type: 'just-review',
        memoryTrackerId: 1,
        title: 'Future note',
      })
      .mockResolvedValueOnce({
        type: 'none',
        message: '0 notes to recall today',
      })
    mockMarkAsRecalled.mockResolvedValue(undefined)

    await processInput('/recall')
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Load more from next 3 days? (y/n)')
    )
    logSpy.mockClear()

    await processInput('y')
    expect(mockRecallNext).toHaveBeenNthCalledWith(
      1,
      0,
      expect.any(AbortSignal)
    )
    expect(mockRecallNext).toHaveBeenNthCalledWith(
      2,
      3,
      expect.any(AbortSignal)
    )
    expect(logSpy).toHaveBeenCalledWith(expect.stringContaining('Future note'))
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Yes, I remember? (y/n)')
    )
    logSpy.mockClear()

    await processInput('y')
    expect(mockMarkAsRecalled).toHaveBeenCalledWith(1, true)
    expect(logSpy).toHaveBeenCalledWith('Recalled successfully')
    expect(logSpy).toHaveBeenCalledWith('Recalled 1 note')
  })

  test('/recall session: one note, answer y, shows Recalled 1 note', async () => {
    mockRecallNext
      .mockResolvedValueOnce({
        type: 'just-review',
        memoryTrackerId: 1,
        title: 'Note 1',
      })
      .mockResolvedValueOnce({
        type: 'none',
        message: '0 notes to recall today',
      })
    mockMarkAsRecalled.mockResolvedValue(undefined)

    await processInput('/recall')
    expect(logSpy).toHaveBeenCalledWith(expect.stringContaining('Note 1'))
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Yes, I remember? (y/n)')
    )
    logSpy.mockClear()

    await processInput('y')
    expect(mockMarkAsRecalled).toHaveBeenCalledWith(1, true)
    expect(logSpy).toHaveBeenCalledWith('Recalled successfully')
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Load more from next 3 days? (y/n)')
    )
    logSpy.mockClear()

    await processInput('n')
    expect(logSpy).toHaveBeenCalledWith('Recalled 1 note')
  })

  test('/recall session: error in continueRecallSession clears mode and logs', async () => {
    mockRecallNext
      .mockResolvedValueOnce({
        type: 'just-review',
        memoryTrackerId: 1,
        title: 'Note 1',
      })
      .mockRejectedValueOnce(new Error('Network error'))
    mockMarkAsRecalled.mockResolvedValue(undefined)

    await processInput('/recall')
    await processInput('y')
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Network error')
    )
  })

  test.each([
    {
      label: 'just-review',
      recall: {
        type: 'just-review' as const,
        memoryTrackerId: 1,
        title: 'Note 1',
      },
    },
    {
      label: 'pending load more',
      recall: {
        type: 'none' as const,
        message: '0 notes to recall today',
      },
    },
  ])('/stop when in recall ($label) exits recall mode', async ({ recall }) => {
    mockRecallNext.mockResolvedValue(recall)
    await processInput('/recall')
    expect(isInRecallSubstate()).toBe(true)
    await processInput('/stop')
    expect(logSpy).toHaveBeenCalledWith('Stopped recall')
    expect(isInRecallSubstate()).toBe(false)
  })

  test('/stop when not in recall substate shows Not supported', async () => {
    await processInput('/stop')
    expect(logSpy).toHaveBeenCalledWith('Not supported')
    expect(isInRecallSubstate()).toBe(false)
  })

  test('in recall state top-level commands are not available', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'just-review',
      memoryTrackerId: 1,
      title: 'Note 1',
    })

    await processInput('/recall')
    expect(isInRecallSubstate()).toBe(true)

    await processInput('/help')
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Type /stop to exit recall')
    )
    expect(logSpy).not.toHaveBeenCalledWith(
      expect.stringContaining('Subcommands')
    )

    logSpy.mockClear()
    await processInput('/recall-status')
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Type /stop to exit recall')
    )
    expect(logSpy).not.toHaveBeenCalledWith(
      expect.stringMatching(/notes to recall today/)
    )
  })

  test('MCQ: prompt with markdown stem shows ANSI codes', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'mcq',
      recallPromptId: 100,
      stem: 'What is **2+2**?',
      choices: ['4', '3', '5'],
    })

    await processInput('/recall')
    const stemCall = logSpy.mock.calls.find(
      (c) =>
        typeof c[0] === 'string' &&
        (c[0] as string).includes('2+2') &&
        !(c[0] as string).includes('  1.')
    )
    expect(stemCall).toBeDefined()
    const output = stemCall![0] as string
    expect(output).toContain('\x1b[')
    expect(output).not.toContain('**')
    expect(stripAnsi(output)).toContain('2+2')
  })

  test('MCQ: prompt with markdown choices shows ANSI codes', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'mcq',
      recallPromptId: 100,
      stem: 'Pick one',
      choices: ['*A*', '**B**', '`C`'],
    })

    await processInput('/recall')
    const choiceCalls = logSpy.mock.calls.filter(
      (c) =>
        typeof c[0] === 'string' &&
        ((c[0] as string).includes('  1.') ||
          (c[0] as string).includes('  2.') ||
          (c[0] as string).includes('  3.'))
    )
    expect(choiceCalls.length).toBeGreaterThanOrEqual(3)
    const allChoices = choiceCalls.map((c) => c[0]).join(' ')
    expect(allChoices).toContain('\x1b[')
    expect(allChoices).not.toContain('*A*')
    expect(allChoices).not.toContain('**B**')
    expect(allChoices).not.toContain('`C`')
    expect(stripAnsi(allChoices)).toContain('A')
    expect(stripAnsi(allChoices)).toContain('B')
    expect(stripAnsi(allChoices)).toContain('C')
  })

  test('/contest when in recall with MCQ contests and shows new question; regenerated MCQ answer passes thinkingTimeMs', async () => {
    vi.useFakeTimers()
    mockRecallNext
      .mockResolvedValueOnce({
        type: 'mcq',
        recallPromptId: 100,
        stem: 'First question?',
        choices: ['A', 'B', 'C'],
      })
      .mockResolvedValueOnce({
        type: 'none',
        message: '0 notes to recall today',
      })
    mockContestAndRegenerate.mockResolvedValue({
      ok: true,
      result: {
        type: 'mcq',
        recallPromptId: 200,
        stem: 'Regenerated question?',
        choices: ['X', 'Y', 'Z'],
      },
    })
    mockAnswerQuiz.mockResolvedValue({ correct: true })

    await processInput('/recall')
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('First question?')
    )
    logSpy.mockClear()

    await processInput('/contest')
    expect(mockContestAndRegenerate).toHaveBeenCalledWith(
      100,
      expect.any(AbortSignal)
    )
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Regenerated question?')
    )
    expect(logSpy).toHaveBeenCalledWith(expect.stringContaining('  1. X'))
    expect(logSpy).toHaveBeenCalledWith(expect.stringContaining('  2. Y'))
    expect(logSpy).toHaveBeenCalledWith(expect.stringContaining('  3. Z'))
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Enter your choice (1-3):')
    )
    logSpy.mockClear()

    vi.advanceTimersByTime(3000)
    await processInput('1')
    expect(mockAnswerQuiz).toHaveBeenCalledWith(200, 0, 3000)
    expect(logSpy).toHaveBeenCalledWith('Correct!')
    expect(logSpy).toHaveBeenCalledWith('Recalled successfully')
  })

  test('/contest when no question pending shows /stop hint', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'just-review',
      memoryTrackerId: 1,
      title: 'Note 1',
    })

    await processInput('/recall')
    await processInput('/contest')
    expect(mockContestAndRegenerate).not.toHaveBeenCalled()
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Type /stop to exit recall')
    )
  })

  test('/contest when contest fails shows error message', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'mcq',
      recallPromptId: 100,
      stem: 'Q?',
      choices: ['A', 'B'],
    })
    mockContestAndRegenerate.mockResolvedValue({
      ok: false,
      message: 'Question could not be regenerated',
    })

    await processInput('/recall')
    expect(isInRecallSubstate()).toBe(true)
    await processInput('/contest')
    expect(logSpy).toHaveBeenCalledWith('Question could not be regenerated')
    expect(isInRecallSubstate()).toBe(true)
  })

  // contract: explicit OutputAdapter (userNotice path; not default console.log)

  test('/contest abort logs Cancelled by user. as user notice', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'mcq',
      recallPromptId: 100,
      stem: 'Q?',
      choices: ['A', 'B'],
    })
    mockContestAndRegenerate.mockRejectedValue(userAbortError())
    const output = {
      log: vi.fn(),
      logError: vi.fn(),
      logUserNotice: vi.fn(),
    }

    await processInput('/recall', output)
    await processInput('/contest', output)

    expect(output.logUserNotice).toHaveBeenCalledWith('Cancelled by user.')
    expect(output.logError).not.toHaveBeenCalled()
  })
})
