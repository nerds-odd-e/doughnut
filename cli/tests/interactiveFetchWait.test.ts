import { describe, test, expect, vi, beforeEach } from 'vitest'
import { processInput, resetRecallStateForTesting } from '../src/interactive.js'
import {
  getInteractiveFetchWaitLine,
  INTERACTIVE_FETCH_WAIT_LINES,
  runInteractiveFetchWait,
} from '../src/interactiveFetchWait.js'
import {
  CLI_USER_ABORTED_WAIT_MESSAGE,
  userAbortError,
  userVisibleOutcomeFromCommandError,
} from '../src/fetchAbort.js'
import {
  buildCurrentPromptSeparatorForStageBand,
  CURRENT_STAGE_BAND_BACKGROUND_SGR,
  formatCurrentStageIndicatorLine,
  formatInteractiveCommandLineInkRows,
  interactiveFetchWaitStageIndicatorLine,
  stripAnsi,
  GREY,
  INTERACTIVE_FETCH_WAIT_PROMPT_FG,
} from '../src/renderer.js'

const { mockAddAccessToken } = vi.hoisted(() => ({
  mockAddAccessToken: vi.fn(),
}))
vi.mock('../src/commands/accessToken.js', async (importOriginal) => {
  const actual =
    await importOriginal<typeof import('../src/commands/accessToken.js')>()
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
  test('stage band and grey command-line paint rows', () => {
    const waitLine = INTERACTIVE_FETCH_WAIT_LINES.addAccessToken
    expect(stripAnsi(interactiveFetchWaitStageIndicatorLine(waitLine))).toBe(
      waitLine
    )

    const width = 80
    const label = interactiveFetchWaitStageIndicatorLine(waitLine)
    const bandTop = formatCurrentStageIndicatorLine(label, width)
    const bandSep = buildCurrentPromptSeparatorForStageBand(width)
    expect(bandTop).toContain(CURRENT_STAGE_BAND_BACKGROUND_SGR)
    expect(bandTop).toContain(INTERACTIVE_FETCH_WAIT_PROMPT_FG)
    expect(stripAnsi(bandTop)).toContain(waitLine)
    expect(bandSep).toContain(CURRENT_STAGE_BAND_BACKGROUND_SGR)
    expect(bandSep).toContain('\x1b[32m')

    const rows = formatInteractiveCommandLineInkRows('', width, 0, {
      placeholderContext: 'interactiveFetchWait',
    })
    expect(rows[0]).toContain(GREY)
    expect(stripAnsi(rows[0]!)).toContain('loading')

    expect(
      stripAnsi(
        interactiveFetchWaitStageIndicatorLine(
          INTERACTIVE_FETCH_WAIT_LINES.createAccessToken
        )
      )
    ).toContain(INTERACTIVE_FETCH_WAIT_LINES.createAccessToken)
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
        INTERACTIVE_FETCH_WAIT_LINES.addAccessToken,
        async () => {
          throw new Error('fail')
        }
      )
    ).rejects.toThrow('fail')
    expect(getInteractiveFetchWaitLine()).toBe(null)
  })
})
