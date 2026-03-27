import {
  describe,
  test,
  expect,
  vi,
  beforeEach,
  afterEach,
  beforeAll,
} from 'vitest'
import { RecallsController, UserController } from 'doughnut-api'
import makeMe from 'doughnut-test-fixtures/makeMe'
import {
  mcqRecallPrompt,
  spellingRecallPrompt,
} from '../recallPromptFixtures.js'
import { addAccessToken } from '../../src/commands/accessToken.js'
import {
  CLI_USER_ABORTED_WAIT_MESSAGE,
  userAbortError,
} from '../../src/fetchAbort.js'
import {
  mockAnswerQuiz,
  mockContestAndRegenerate,
  mockMarkAsRecalled,
  mockRecallNext,
} from './interactiveRecallMockAccess.js'
import { mkdtempSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { cancelInteractiveFetchWaitFor } from '../../src/interactiveFetchWait.js'
import { RECALL_LOAD_CLI_TEST_DELAY_MS_ENV } from '../../src/commands/recall.js'
import {
  isInRecallSubstate,
  processInput,
  resetRecallStateForTesting,
} from '../../src/interactive.js'
import { stripAnsi } from '../../src/renderer.js'
import { makeTempConfigDir, withConfigDir } from './interactiveTestHelpers.js'
import { recallNextQuestion } from '../recallNextTestShapes.js'

// Contract: processInput + default console adapter (log / writeCurrentPrompt). Not TTY bytes — see interactiveTty*.test.ts and .cursor/rules/cli.mdc → Vitest.

vi.mock('doughnut-api', () => ({
  getApiConfig: () => ({ apiBaseUrl: 'http://localhost:9081' }),
  configureClient: vi.fn(),
  MemoryTrackerController: {
    askAQuestion: vi.fn(),
    markAsRecalled: vi.fn(),
    showMemoryTracker: vi.fn(),
  },
  RecallsController: {
    recalling: vi.fn(),
  },
  RecallPromptController: {
    answerQuiz: vi.fn(),
    contest: vi.fn(),
    regenerate: vi.fn(),
  },
  UserController: {
    getTokenInfo: vi.fn(),
  },
}))

describe('processInput', () => {
  let logSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    resetRecallStateForTesting()
    mockRecallNext.mockClear()
    mockMarkAsRecalled.mockClear()
    mockAnswerQuiz.mockClear()
    mockContestAndRegenerate.mockClear()
    logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
  })

  afterEach(() => {
    vi.useRealTimers()
    logSpy.mockRestore()
  })

  // contract: default console (not TTY bytes — see interactiveTty*.test.ts)

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

  test('returns false and shows usage for /add-access-token without token', async () => {
    expect(await processInput('/add-access-token')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Usage: /add-access-token <token>')
    logSpy.mockClear()
    expect(await processInput('/add-access-token ')).toBe(false)
    expect(logSpy).toHaveBeenCalledWith('Usage: /add-access-token <token>')
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

  // contract: /recall via processInput — default console (below) and OutputAdapter + real recallNext (load cancel).

  test('/recall: SPELLING question logs Not supported and exits recall mode', async () => {
    mockRecallNext.mockResolvedValue(
      recallNextQuestion(spellingRecallPrompt(100, 'means incite violence'))
    )
    await processInput('/recall')
    expect(logSpy).toHaveBeenCalledWith('Not supported')
    expect(isInRecallSubstate()).toBe(false)
  })

  test('/recall load more: user says n, shows 0 notes to recall today', async () => {
    mockRecallNext.mockResolvedValue({
      type: 'none',
      message: '0 notes to recall today',
    })

    await processInput('/recall')
    expect(logSpy).toHaveBeenCalledWith(
      expect.stringContaining('Load more from next 3 days? (y/n)')
    )
    expect(mockRecallNext).toHaveBeenCalledWith(0, expect.any(AbortSignal))
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
        notebookTitle: 'Notebook',
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
        notebookTitle: 'Notebook',
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
        notebookTitle: 'Notebook',
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
        notebookTitle: 'Notebook',
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
      notebookTitle: 'Notebook',
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

  test('MCQ: stem goes to writeCurrentPrompt and choices to log when hooks differ', async () => {
    mockRecallNext.mockResolvedValue(
      recallNextQuestion(mcqRecallPrompt(101, 'Pick one', ['Alpha', 'Beta']))
    )
    const promptSpy = vi.fn()
    const logSpyLocal = vi.fn()
    const splitOutput = {
      log: logSpyLocal,
      logError: vi.fn(),
      writeCurrentPrompt: promptSpy,
    }

    await processInput('/recall', splitOutput)

    expect(promptSpy).toHaveBeenCalledTimes(2)
    expect(promptSpy.mock.calls[0]![0]).toContain('📓')
    expect(promptSpy.mock.calls[0]![0]).toContain('Notebook')
    expect(promptSpy.mock.calls[1]![0]).toContain('Pick one')
    expect(
      promptSpy.mock.calls.every(
        (c: unknown[]) => !String(c[0]).includes('  1. ')
      )
    ).toBe(true)
    expect(
      logSpyLocal.mock.calls.some((c: unknown[]) =>
        String(c[0]).includes('  1. ')
      )
    ).toBe(true)
    expect(
      logSpyLocal.mock.calls.some((c: unknown[]) =>
        String(c[0]).includes('  2. ')
      )
    ).toBe(true)
    expect(logSpyLocal).toHaveBeenCalledWith('Enter your choice (1-2):')
  })

  test('MCQ: default console renders markdown in stem and choices as ANSI', async () => {
    mockRecallNext.mockResolvedValue(
      recallNextQuestion(
        mcqRecallPrompt(100, 'What is **2+2**?', ['*A*', '**B**', '`C`'])
      )
    )

    await processInput('/recall')
    const stemCall = logSpy.mock.calls.find(
      (c: unknown[]) =>
        typeof c[0] === 'string' &&
        (c[0] as string).includes('2+2') &&
        !(c[0] as string).includes('  1.')
    )
    expect(stemCall).toBeDefined()
    const stemOut = stemCall![0] as string
    expect(stemOut).toContain('\x1b[')
    expect(stemOut).not.toContain('**')
    expect(stripAnsi(stemOut)).toContain('2+2')

    const choiceCalls = logSpy.mock.calls.filter(
      (c: unknown[]) =>
        typeof c[0] === 'string' &&
        ((c[0] as string).includes('  1.') ||
          (c[0] as string).includes('  2.') ||
          (c[0] as string).includes('  3.'))
    )
    expect(choiceCalls.length).toBeGreaterThanOrEqual(3)
    const allChoices = choiceCalls.map((c: unknown[]) => c[0]).join(' ')
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
      .mockResolvedValueOnce(
        recallNextQuestion(
          mcqRecallPrompt(100, 'First question?', ['A', 'B', 'C'])
        )
      )
      .mockResolvedValueOnce({
        type: 'none',
        message: '0 notes to recall today',
      })
    mockContestAndRegenerate.mockResolvedValue({
      ok: true,
      result: recallNextQuestion(
        mcqRecallPrompt(200, 'Regenerated question?', ['X', 'Y', 'Z'])
      ),
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
      notebookTitle: 'Notebook',
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
    mockRecallNext.mockResolvedValue(
      recallNextQuestion(mcqRecallPrompt(100, 'Q?', ['A', 'B']))
    )
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

  describe('contract: /recall load — real recallNext + OutputAdapter cancel', () => {
    let realRecallNext: typeof import('../../src/commands/recall.js')['recallNext']
    let originalConfigDir: string | undefined
    let originalSlow: string | undefined

    beforeAll(async () => {
      const mod = await vi.importActual<
        typeof import('../../src/commands/recall.js')
      >('../../src/commands/recall.js')
      realRecallNext = mod.recallNext
    })

    beforeEach(() => {
      originalConfigDir = process.env.DOUGHNUT_CONFIG_DIR
      process.env.DOUGHNUT_CONFIG_DIR = mkdtempSync(
        `${tmpdir()}/doughnut-processInput-recall-load-`
      )
      originalSlow = process.env[RECALL_LOAD_CLI_TEST_DELAY_MS_ENV]
      process.env[RECALL_LOAD_CLI_TEST_DELAY_MS_ENV] = '60000'
      resetRecallStateForTesting()
      vi.useFakeTimers()
      mockRecallNext.mockImplementation(realRecallNext)
    })

    afterEach(() => {
      vi.useRealTimers()
      mockRecallNext.mockReset()
      if (originalConfigDir === undefined) {
        delete process.env.DOUGHNUT_CONFIG_DIR
      } else {
        process.env.DOUGHNUT_CONFIG_DIR = originalConfigDir
      }
      if (originalSlow === undefined) {
        delete process.env[RECALL_LOAD_CLI_TEST_DELAY_MS_ENV]
      } else {
        process.env[RECALL_LOAD_CLI_TEST_DELAY_MS_ENV] = originalSlow
      }
    })

    test('cancel during CLI test delay logs Cancelled by user. (real recallNext)', async () => {
      vi.mocked(RecallsController.recalling).mockResolvedValue({
        data: makeMe.aDueMemoryTrackersList.toRepeat([]).please(),
      } as never)
      vi.mocked(UserController.getTokenInfo).mockResolvedValue({
        data: { id: 1, label: 'Test Token' },
      } as never)
      await addAccessToken('test-token')

      const out = {
        log: vi.fn(),
        logError: vi.fn(),
        writeCurrentPrompt: vi.fn(),
        beginCurrentPrompt: vi.fn(),
        onInteractiveFetchWaitChanged: vi.fn(),
      }
      const done = processInput('/recall', out)
      await vi.waitFor(() =>
        expect(out.onInteractiveFetchWaitChanged).toHaveBeenCalled()
      )
      expect(cancelInteractiveFetchWaitFor(out)).toBe(true)
      await done
      expect(out.log).toHaveBeenCalledWith(CLI_USER_ABORTED_WAIT_MESSAGE)
      expect(RecallsController.recalling).not.toHaveBeenCalled()
      expect(out.onInteractiveFetchWaitChanged).toHaveBeenCalledTimes(2)
    })
  })

  // contract: explicit OutputAdapter (userNotice path; not default console.log)

  test('/contest abort logs Cancelled by user. as user notice', async () => {
    mockRecallNext.mockResolvedValue(
      recallNextQuestion(mcqRecallPrompt(100, 'Q?', ['A', 'B']))
    )
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
