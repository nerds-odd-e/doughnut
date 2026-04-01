import { render } from 'ink-testing-library'
import { describe, expect, test } from 'vitest'
import { AsyncAssistantFetchStage } from '../src/commands/gmail/AsyncAssistantFetchStage.js'
import {
  pressEscapeAndWait,
  StageKeyRoot,
  waitForFrames,
} from './inkTestHelpers.js'

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
    let aborted: string | null = null
    const { stdin, frames } = render(
      <StageKeyRoot>
        <AsyncAssistantFetchStage
          spinnerLabel="Loading test…"
          runAssistantMessage={(signal) => pendingUntilAbort(signal)}
          onSettled={(t) => {
            settled = t
          }}
          onAbortWithError={(t) => {
            aborted = t
          }}
        />
      </StageKeyRoot>
    )

    await waitForFrames(
      () => frames.join('\n'),
      (c) => c.includes('Loading test')
    )

    await pressEscapeAndWait(
      stdin,
      () => frames.join('\n'),
      () => aborted !== null
    )

    expect(aborted).toBe('Cancelled.')
    expect(settled).toBeNull()
  })

  test('unmount while pending settles once with Cancelled', async () => {
    let successCount = 0
    let abortCount = 0
    let lastAbort: string | null = null
    const { frames, unmount } = render(
      <StageKeyRoot>
        <AsyncAssistantFetchStage
          spinnerLabel="Hold…"
          runAssistantMessage={(signal) => pendingUntilAbort(signal)}
          onSettled={() => {
            successCount += 1
          }}
          onAbortWithError={(t) => {
            abortCount += 1
            lastAbort = t
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
      () => `${abortCount}:${lastAbort ?? ''}`,
      () => abortCount >= 1
    )

    expect(successCount).toBe(0)
    expect(abortCount).toBe(1)
    expect(lastAbort).toBe('Cancelled.')
  })
})
