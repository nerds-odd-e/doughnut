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
  applyCommandInputPaintChrome,
  buildCommandInputDraftLines,
  buildCurrentPromptSeparatorForStageBand,
  CURRENT_STAGE_BAND_BACKGROUND_SGR,
  formatBorderlessCommandInputPaintLines,
  formatCurrentStageIndicatorLine,
  interactiveFetchWaitStageIndicatorLine,
  isGreyDisabledInputChrome,
  stripAnsi,
  GREY,
  INTERACTIVE_FETCH_WAIT_PROMPT_FG,
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
  test('renderer: fetch-wait stage label, grey chrome contexts, live region colors', () => {
    const recallLine = INTERACTIVE_FETCH_WAIT_LINES.recallNext
    expect(stripAnsi(interactiveFetchWaitStageIndicatorLine(recallLine))).toBe(
      recallLine
    )

    expect(isGreyDisabledInputChrome('interactiveFetchWait')).toBe(true)
    expect(isGreyDisabledInputChrome('tokenList')).toBe(true)
    expect(isGreyDisabledInputChrome('default')).toBe(false)
    expect(isGreyDisabledInputChrome('recallMcq')).toBe(false)

    const draft = buildCommandInputDraftLines('', 80, {
      placeholderContext: 'interactiveFetchWait',
    })[0]!
    expect(draft).not.toContain('→')
    expect(draft).toContain('loading ...')

    const width = 80
    const label = interactiveFetchWaitStageIndicatorLine(recallLine)
    const bandTop = formatCurrentStageIndicatorLine(label, width)
    const bandSep = buildCurrentPromptSeparatorForStageBand(width)
    expect(bandTop).toContain(CURRENT_STAGE_BAND_BACKGROUND_SGR)
    expect(bandTop).toContain(INTERACTIVE_FETCH_WAIT_PROMPT_FG)
    expect(stripAnsi(bandTop)).toContain(recallLine)
    expect(bandSep).toContain(CURRENT_STAGE_BAND_BACKGROUND_SGR)
    expect(bandSep).toContain('\x1b[32m')

    const paint = applyCommandInputPaintChrome(
      formatBorderlessCommandInputPaintLines(
        buildCommandInputDraftLines('', width, {
          placeholderContext: 'interactiveFetchWait',
        }),
        width
      ),
      { placeholderContext: 'interactiveFetchWait' }
    )
    expect(paint[0]).toContain(GREY)

    expect(
      stripAnsi(
        interactiveFetchWaitStageIndicatorLine(
          INTERACTIVE_FETCH_WAIT_LINES.recallStatus
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
