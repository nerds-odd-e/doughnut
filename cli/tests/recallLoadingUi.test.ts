import { describe, test, expect, vi, beforeEach } from 'vitest'
import {
  LOADING_MESSAGES,
  processInput,
  resetRecallStateForTesting,
} from '../src/interactive.js'
import {
  buildBoxLines,
  buildLiveRegionLines,
  formatLoadingPromptWithEllipsis,
  isInputBoxDisabledPlaceholderContext,
  LOADING_FOREGROUND,
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
    notifyLoadingChanged: vi.fn(),
  }
}

beforeEach(() => {
  resetRecallStateForTesting()
  mockRecallNext.mockReset()
})

describe('recall loading UI', () => {
  test('renderer: ellipsis cycle, disabled contexts, grey live region for loading', () => {
    expect(formatLoadingPromptWithEllipsis('Wait', 0)).toBe('Wait.')
    expect(formatLoadingPromptWithEllipsis('Wait', 1)).toBe('Wait..')
    expect(formatLoadingPromptWithEllipsis('Wait', 2)).toBe('Wait...')
    expect(formatLoadingPromptWithEllipsis('Wait', 3)).toBe('Wait.')

    expect(isInputBoxDisabledPlaceholderContext('loading')).toBe(true)
    expect(isInputBoxDisabledPlaceholderContext('tokenList')).toBe(true)
    expect(isInputBoxDisabledPlaceholderContext('default')).toBe(false)
    expect(isInputBoxDisabledPlaceholderContext('recallMcq')).toBe(false)

    const boxLine = buildBoxLines('', 80, { placeholderContext: 'loading' })[0]!
    expect(boxLine).not.toContain('→')
    expect(boxLine).toContain('loading ...')

    const prompt = formatLoadingPromptWithEllipsis(
      LOADING_MESSAGES.recallNext,
      1
    )
    const live = buildLiveRegionLines('', 80, [prompt], [], [], {
      placeholderContext: 'loading',
      currentPromptSgr: LOADING_FOREGROUND,
    })
    expect(live[0]).toContain('\x1b[32m')
    expect(live[1]).toContain(LOADING_FOREGROUND)
    expect(stripAnsi(live[1])).toContain(LOADING_MESSAGES.recallNext)
    const boxTopIdx = live.findIndex((l) => stripAnsi(l).startsWith('┌'))
    expect(boxTopIdx).toBeGreaterThan(0)
    expect(live[boxTopIdx]).toContain(GREY)
  })

  test('processInput /recall calls notifyLoadingChanged once when loading starts and once when recallNext settles', async () => {
    let resolveRecall!: (value: RecallNextResult) => void
    mockRecallNext.mockImplementation(
      () =>
        new Promise<RecallNextResult>((resolve) => {
          resolveRecall = resolve
        })
    )
    const out = outputAdapter()
    const finished = processInput('/recall', out)
    await vi.waitFor(() => expect(out.notifyLoadingChanged).toHaveBeenCalled())
    expect(out.notifyLoadingChanged).toHaveBeenCalledTimes(1)
    resolveRecall({ type: 'none', message: '0' })
    await finished
    expect(out.notifyLoadingChanged).toHaveBeenCalledTimes(2)

    resetRecallStateForTesting()
    mockRecallNext.mockRejectedValueOnce(new Error('network'))
    const outErr = outputAdapter()
    await processInput('/recall', outErr)
    expect(outErr.notifyLoadingChanged).toHaveBeenCalledTimes(2)
    expect(outErr.logError).toHaveBeenCalled()
  })
})
