import { describe, test, expect, vi, beforeEach } from 'vitest'
import {
  FETCH_WAIT_LINES,
  RECALL_FETCH_WAIT_BASE_LINE,
  processInput,
  resetRecallStateForTesting,
  withInteractiveFetchWaitUi,
} from '../src/interactive.js'
import {
  buildBoxLines,
  buildLiveRegionLines,
  formatRecallFetchWaitPromptLine,
  isGreyDisabledInputChrome,
  RECALL_FETCH_WAIT_PROMPT_FG,
  stripAnsi,
  GREY,
} from '../src/renderer.js'
import type { RecallNextResult } from '../src/recall.js'

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

function outputAdapter() {
  return {
    log: vi.fn(),
    logError: vi.fn(),
    writeCurrentPrompt: vi.fn(),
    beginCurrentPrompt: vi.fn(),
    onRecallFetchWaitChanged: vi.fn(),
  }
}

beforeEach(() => {
  resetRecallStateForTesting()
  mockRecallNext.mockReset()
  mockRecallStatus.mockReset()
  mockAddAccessToken.mockReset()
})

describe('recall fetch wait UI', () => {
  test('renderer: ellipsis, grey chrome contexts, live region colors', () => {
    expect(formatRecallFetchWaitPromptLine('Wait', 0)).toBe('Wait.')
    expect(formatRecallFetchWaitPromptLine('Wait', 3)).toBe('Wait.')

    expect(isGreyDisabledInputChrome('recallFetchWait')).toBe(true)
    expect(isGreyDisabledInputChrome('tokenList')).toBe(true)
    expect(isGreyDisabledInputChrome('default')).toBe(false)
    expect(isGreyDisabledInputChrome('recallMcq')).toBe(false)

    const boxLine = buildBoxLines('', 80, {
      placeholderContext: 'recallFetchWait',
    })[0]!
    expect(boxLine).not.toContain('→')
    expect(boxLine).toContain('loading ...')

    const prompt = formatRecallFetchWaitPromptLine(
      RECALL_FETCH_WAIT_BASE_LINE,
      1
    )
    const live = buildLiveRegionLines('', 80, [prompt], [], [], {
      placeholderContext: 'recallFetchWait',
      currentPromptSgr: RECALL_FETCH_WAIT_PROMPT_FG,
    })
    expect(live[0]).toContain('\x1b[32m')
    expect(live[1]).toContain(RECALL_FETCH_WAIT_PROMPT_FG)
    expect(stripAnsi(live[1])).toContain(RECALL_FETCH_WAIT_BASE_LINE)
    const boxTopIdx = live.findIndex((l) => stripAnsi(l).startsWith('┌'))
    expect(boxTopIdx).toBeGreaterThan(0)
    expect(live[boxTopIdx]).toContain(GREY)

    expect(
      stripAnsi(
        formatRecallFetchWaitPromptLine(FETCH_WAIT_LINES.recallStatus, 0)
      )
    ).toContain(FETCH_WAIT_LINES.recallStatus)
  })

  test('processInput /recall signals TTY once when recall fetch starts and once when it ends', async () => {
    let resolveRecall!: (value: RecallNextResult) => void
    mockRecallNext.mockImplementation(
      () =>
        new Promise<RecallNextResult>((resolve) => {
          resolveRecall = resolve
        })
    )
    const out = outputAdapter()
    const finished = processInput('/recall', out)
    await vi.waitFor(() =>
      expect(out.onRecallFetchWaitChanged).toHaveBeenCalled()
    )
    expect(out.onRecallFetchWaitChanged).toHaveBeenCalledTimes(1)
    resolveRecall({ type: 'none', message: '0' })
    await finished
    expect(out.onRecallFetchWaitChanged).toHaveBeenCalledTimes(2)

    resetRecallStateForTesting()
    mockRecallNext.mockRejectedValueOnce(new Error('network'))
    const outErr = outputAdapter()
    await processInput('/recall', outErr)
    expect(outErr.onRecallFetchWaitChanged).toHaveBeenCalledTimes(2)
    expect(outErr.logError).toHaveBeenCalled()
  })

  test('processInput /recall-status signals fetch wait start and end', async () => {
    let resolveStatus!: (value: string) => void
    mockRecallStatus.mockImplementation(
      () =>
        new Promise<string>((resolve) => {
          resolveStatus = resolve
        })
    )
    const out = outputAdapter()
    const done = processInput('/recall-status', out)
    await vi.waitFor(() =>
      expect(out.onRecallFetchWaitChanged).toHaveBeenCalled()
    )
    expect(out.onRecallFetchWaitChanged).toHaveBeenCalledTimes(1)
    resolveStatus('2 notes to recall today')
    await done
    expect(out.onRecallFetchWaitChanged).toHaveBeenCalledTimes(2)
    expect(out.log).toHaveBeenCalledWith('2 notes to recall today')
  })

  test('processInput /add-access-token signals fetch wait start and end', async () => {
    let resolveAdd!: () => void
    mockAddAccessToken.mockImplementation(
      () =>
        new Promise<void>((resolve) => {
          resolveAdd = resolve
        })
    )
    const out = outputAdapter()
    const done = processInput('/add-access-token secret', out)
    await vi.waitFor(() =>
      expect(out.onRecallFetchWaitChanged).toHaveBeenCalled()
    )
    expect(out.onRecallFetchWaitChanged).toHaveBeenCalledTimes(1)
    resolveAdd()
    await done
    expect(out.onRecallFetchWaitChanged).toHaveBeenCalledTimes(2)
    expect(mockAddAccessToken).toHaveBeenCalledWith('secret')
  })

  test('withInteractiveFetchWaitUi notifies end wait after rejection', async () => {
    const out = outputAdapter()
    await expect(
      withInteractiveFetchWaitUi(out, 'Busy', async () => {
        throw new Error('fail')
      })
    ).rejects.toThrow('fail')
    expect(out.onRecallFetchWaitChanged).toHaveBeenCalledTimes(2)
  })
})
