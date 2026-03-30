import { useCallback, useRef, type ReactNode } from 'react'
import type { Key } from 'ink'
import { Box, useInput } from 'ink'
import { render } from 'ink-testing-library'
import { describe, expect, test } from 'vitest'
import { AsyncAssistantFetchStage } from '../src/commands/gmail/AsyncAssistantFetchStage.js'
import type { StageKeyHandler } from '../src/commands/accessToken/stageKeyForwardContext.js'
import { SetStageKeyHandlerContext } from '../src/commands/accessToken/stageKeyForwardContext.js'
import { waitForFrames } from './inkTestHelpers.js'

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

/** Mirrors InteractiveCliApp stage key forwarding. */
function StageKeyRoot({ children }: { readonly children: ReactNode }) {
  const stageKeyHandlerRef = useRef<StageKeyHandler | null>(null)
  const setStageKeyHandler = useCallback((handler: StageKeyHandler | null) => {
    stageKeyHandlerRef.current = handler
  }, [])
  useInput(
    useCallback((input: string, key: Key) => {
      stageKeyHandlerRef.current?.(input, key)
    }, [])
  )
  return (
    <SetStageKeyHandlerContext.Provider value={setStageKeyHandler}>
      <Box>{children}</Box>
    </SetStageKeyHandlerContext.Provider>
  )
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
