import { render } from 'ink-testing-library'
import { describe, expect, test } from 'vitest'
import { AsyncAssistantFetchStage } from '../src/commands/gmail/AsyncAssistantFetchStage.js'
import { StageKeyRoot, waitForFrames } from './inkTestHelpers.js'

function pendingUntilAbort(signal: AbortSignal): Promise<string> {
  return new Promise<string>((_, reject) => {
    signal.addEventListener(
      'abort',
      () => {
        reject(new DOMException('Aborted', 'AbortError'))
      },
      { once: true }
    )
  })
}

describe('AsyncAssistantFetchStage', () => {
  test('Escape settles with Cancelled when work listens to signal', async () => {
    let settled: string | null = null
    const { stdin, frames } = render(
      <StageKeyRoot>
        <AsyncAssistantFetchStage
          spinnerLabel="Loading test…"
          runAssistantMessage={(signal) => pendingUntilAbort(signal)}
          onSettled={(t) => {
            settled = t
          }}
        />
      </StageKeyRoot>
    )

    await waitForFrames(
      () => frames.join('\n'),
      (c) => c.includes('Loading test')
    )

    stdin.write('\u001b')
    await waitForFrames(
      () => frames.join('\n'),
      () => settled !== null
    )

    expect(settled).toBe('Cancelled.')
  })

  test('unmount while pending settles once with Cancelled', async () => {
    let settleCount = 0
    let lastSettled: string | null = null
    const { frames, unmount } = render(
      <StageKeyRoot>
        <AsyncAssistantFetchStage
          spinnerLabel="Hold…"
          runAssistantMessage={(signal) => pendingUntilAbort(signal)}
          onSettled={(t) => {
            settleCount += 1
            lastSettled = t
          }}
        />
      </StageKeyRoot>
    )

    await waitForFrames(
      () => frames.join('\n'),
      (c) => c.includes('Hold')
    )

    unmount()
    await waitForFrames(
      () => `${settleCount}:${lastSettled ?? ''}`,
      () => settleCount >= 1
    )

    expect(settleCount).toBe(1)
    expect(lastSettled).toBe('Cancelled.')
  })
})
