import { describe, test, expect, vi, beforeEach } from 'vitest'
import {
  INTERACTIVE_FETCH_WAIT_LINES,
  processInput,
  resetRecallStateForTesting,
  runInteractiveFetchWait,
  getInteractiveFetchWaitLine,
} from '../src/interactive.js'
import {
  CLI_USER_ABORTED_WAIT_MESSAGE,
  userAbortError,
  userVisibleOutcomeFromCommandError,
} from '../src/fetchAbort.js'
import {
  buildBoxLines,
  buildLiveRegionLines,
  CURRENT_STAGE_BAND_BACKGROUND_SGR,
  formatInteractiveFetchWaitPromptLine,
  isGreyDisabledInputChrome,
  stripAnsi,
  GREY,
  INTERACTIVE_FETCH_WAIT_PROMPT_FG,
  RESET,
} from '../src/renderer.js'

const { mockRecallNext, mockRecallStatus } = vi.hoisted(() => ({
  mockRecallNext: vi.fn(),
  mockRecallStatus: vi.fn(),
}))
const { mockAddAccessToken } = vi.hoisted(() => ({
  mockAddAccessToken: vi.fn(),
}))
vi.mock('../src/recall.js', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../src/recall.js')>()
  return {
    ...actual,
    recallNext: mockRecallNext,
    recallStatus: mockRecallStatus,
  }
})
vi.mock('../src/accessToken.js', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../src/accessToken.js')>()
  return { ...actual, addAccessToken: mockAddAccessToken }
})

function minimalOutputAdapter() {
  return {
    log: vi.fn(),
    logError: vi.fn(),
    logUserNotice: vi.fn(),
  }
}

beforeEach(() => {
  resetRecallStateForTesting()
  mockRecallNext.mockReset()
  mockRecallStatus.mockReset()
  mockAddAccessToken.mockReset()
})

describe('userVisibleOutcomeFromCommandError', () => {
  test('AbortError maps to user notice text and tone', () => {
    expect(userVisibleOutcomeFromCommandError(userAbortError())).toEqual({
      text: CLI_USER_ABORTED_WAIT_MESSAGE,
      tone: 'userNotice',
    })
  })

  test('other errors map to message and error tone', () => {
    expect(userVisibleOutcomeFromCommandError(new Error('boom'))).toEqual({
      text: 'boom',
      tone: 'error',
    })
  })
})

describe('interactive fetch wait UI', () => {
  test('renderer: ellipsis, grey chrome contexts, live region colors', () => {
    const recallLine = INTERACTIVE_FETCH_WAIT_LINES.recallNext
    expect(formatInteractiveFetchWaitPromptLine(recallLine, 0)).toBe(
      `${recallLine}.`
    )
    expect(formatInteractiveFetchWaitPromptLine(recallLine, 3)).toBe(
      `${recallLine}.`
    )

    expect(isGreyDisabledInputChrome('interactiveFetchWait')).toBe(true)
    expect(isGreyDisabledInputChrome('tokenList')).toBe(true)
    expect(isGreyDisabledInputChrome('default')).toBe(false)
    expect(isGreyDisabledInputChrome('recallMcq')).toBe(false)

    const boxLine = buildBoxLines('', 80, {
      placeholderContext: 'interactiveFetchWait',
    })[0]!
    expect(boxLine).not.toContain('→')
    expect(boxLine).toContain('loading ...')

    const label = `${INTERACTIVE_FETCH_WAIT_PROMPT_FG}${formatInteractiveFetchWaitPromptLine(recallLine, 1)}${RESET}`
    const live = buildLiveRegionLines('', 80, [], [], [label], {
      placeholderContext: 'interactiveFetchWait',
    })
    expect(live[0]).toContain(CURRENT_STAGE_BAND_BACKGROUND_SGR)
    expect(live[0]).toContain(INTERACTIVE_FETCH_WAIT_PROMPT_FG)
    expect(stripAnsi(live[0])).toContain(recallLine)
    expect(live[1]).toContain(CURRENT_STAGE_BAND_BACKGROUND_SGR)
    expect(live[1]).toContain('\x1b[32m')
    const boxTopIdx = live.findIndex((l) => stripAnsi(l).startsWith('┌'))
    expect(boxTopIdx).toBe(2)
    expect(live[boxTopIdx]).toContain(GREY)

    expect(
      stripAnsi(
        formatInteractiveFetchWaitPromptLine(
          INTERACTIVE_FETCH_WAIT_LINES.recallStatus,
          0
        )
      )
    ).toContain(INTERACTIVE_FETCH_WAIT_LINES.recallStatus)
  })

  test('processInput /recall: rejected recallNext surfaces logError', async () => {
    mockRecallNext.mockRejectedValueOnce(new Error('network'))
    const out = minimalOutputAdapter()
    await processInput('/recall', out)
    expect(out.logError).toHaveBeenCalled()
  })

  test('processInput /recall-status resolves and logs status text', async () => {
    mockRecallStatus.mockResolvedValueOnce('2 notes to recall today')
    const out = minimalOutputAdapter()
    await processInput('/recall-status', out)
    expect(out.log).toHaveBeenCalledWith('2 notes to recall today')
  })

  test('processInput /add-access-token passes AbortSignal and logs Token added', async () => {
    let resolveAdd!: () => void
    mockAddAccessToken.mockImplementation(
      () =>
        new Promise<void>((resolve) => {
          resolveAdd = resolve
        })
    )
    const out = minimalOutputAdapter()
    const done = processInput('/add-access-token secret', out)
    resolveAdd()
    await done
    expect(mockAddAccessToken).toHaveBeenCalledWith(
      'secret',
      expect.any(AbortSignal)
    )
    expect(out.log).toHaveBeenCalledWith('Token added')
  })

  test('runInteractiveFetchWait clears active wait line after rejection', async () => {
    const out = minimalOutputAdapter()
    await expect(
      runInteractiveFetchWait(
        out,
        INTERACTIVE_FETCH_WAIT_LINES.recallNext,
        async () => {
          throw new Error('fail')
        }
      )
    ).rejects.toThrow('fail')
    expect(getInteractiveFetchWaitLine()).toBe(null)
  })
})
