import { describe, test, expect, vi, beforeEach } from 'vitest'
import {
  RECALL_FETCH_WAIT_BASE_LINE,
  processInput,
  resetRecallStateForTesting,
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

const { mockRecallNext } = vi.hoisted(() => ({
  mockRecallNext: vi.fn(),
}))
vi.mock('../src/recall.js', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../src/recall.js')>()
  return { ...actual, recallNext: mockRecallNext }
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
})
