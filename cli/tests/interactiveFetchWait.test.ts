import { describe, test, expect, vi, beforeEach } from 'vitest'
import {
  INTERACTIVE_FETCH_WAIT_LINES,
  processInput,
  resetRecallStateForTesting,
  runInteractiveFetchWait,
} from '../src/interactive.js'
import { cancelInFlightRecallNextFetchFor } from '../src/interactiveFetchWait.js'
import {
  buildBoxLines,
  buildLiveRegionLines,
  formatInteractiveFetchWaitPromptLine,
  isGreyDisabledInputChrome,
  INTERACTIVE_FETCH_WAIT_PROMPT_FG,
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
    onInteractiveFetchWaitChanged: vi.fn(),
  }
}

beforeEach(() => {
  resetRecallStateForTesting()
  mockRecallNext.mockReset()
  mockRecallStatus.mockReset()
  mockAddAccessToken.mockReset()
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

    const prompt = formatInteractiveFetchWaitPromptLine(recallLine, 1)
    const live = buildLiveRegionLines('', 80, [prompt], [], [], {
      placeholderContext: 'interactiveFetchWait',
      currentPromptSgr: INTERACTIVE_FETCH_WAIT_PROMPT_FG,
    })
    expect(live[0]).toContain('\x1b[32m')
    expect(live[1]).toContain(INTERACTIVE_FETCH_WAIT_PROMPT_FG)
    expect(stripAnsi(live[1])).toContain(recallLine)
    const boxTopIdx = live.findIndex((l) => stripAnsi(l).startsWith('┌'))
    expect(boxTopIdx).toBeGreaterThan(0)
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

  test('processInput /recall signals TTY once when recall fetch starts and once when it ends', async () => {
    let resolveRecall!: (value: RecallNextResult) => void
    mockRecallNext.mockImplementation(
      (_due, _signal) =>
        new Promise<RecallNextResult>((resolve) => {
          resolveRecall = resolve
        })
    )
    const out = outputAdapter()
    const finished = processInput('/recall', out)
    await vi.waitFor(() =>
      expect(out.onInteractiveFetchWaitChanged).toHaveBeenCalled()
    )
    expect(out.onInteractiveFetchWaitChanged).toHaveBeenCalledTimes(1)
    resolveRecall({ type: 'none', message: '0' })
    await finished
    expect(out.onInteractiveFetchWaitChanged).toHaveBeenCalledTimes(2)

    resetRecallStateForTesting()
    mockRecallNext.mockRejectedValueOnce(new Error('network'))
    const outErr = outputAdapter()
    await processInput('/recall', outErr)
    expect(outErr.onInteractiveFetchWaitChanged).toHaveBeenCalledTimes(2)
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
      expect(out.onInteractiveFetchWaitChanged).toHaveBeenCalled()
    )
    expect(out.onInteractiveFetchWaitChanged).toHaveBeenCalledTimes(1)
    resolveStatus('2 notes to recall today')
    await done
    expect(out.onInteractiveFetchWaitChanged).toHaveBeenCalledTimes(2)
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
      expect(out.onInteractiveFetchWaitChanged).toHaveBeenCalled()
    )
    expect(out.onInteractiveFetchWaitChanged).toHaveBeenCalledTimes(1)
    resolveAdd()
    await done
    expect(out.onInteractiveFetchWaitChanged).toHaveBeenCalledTimes(2)
    expect(mockAddAccessToken).toHaveBeenCalledWith('secret')
  })

  test('runInteractiveFetchWait notifies end wait after rejection', async () => {
    const out = outputAdapter()
    await expect(
      runInteractiveFetchWait(
        out,
        INTERACTIVE_FETCH_WAIT_LINES.recallNext,
        async () => {
          throw new Error('fail')
        }
      )
    ).rejects.toThrow('fail')
    expect(out.onInteractiveFetchWaitChanged).toHaveBeenCalledTimes(2)
  })

  test('/recall: abort during recall load logs cancellation and clears wait', async () => {
    mockRecallNext.mockImplementation((_due, signal) => {
      return new Promise<RecallNextResult>((_resolve, reject) => {
        const onAbort = () => reject(new DOMException('Aborted', 'AbortError'))
        if (signal?.aborted) {
          onAbort()
          return
        }
        signal?.addEventListener('abort', onAbort, { once: true })
      })
    })
    const out = outputAdapter()
    const done = processInput('/recall', out)
    await vi.waitFor(() =>
      expect(out.onInteractiveFetchWaitChanged).toHaveBeenCalled()
    )
    expect(cancelInFlightRecallNextFetchFor(out)).toBe(true)
    await done
    expect(out.log).toHaveBeenCalledWith('Cancelled by user.')
    expect(out.onInteractiveFetchWaitChanged).toHaveBeenCalledTimes(2)
  })
})
