import { render } from 'ink-testing-library'
import {
  createElement,
  useCallback,
  useRef,
  type ReactElement,
  type ReactNode,
} from 'react'
import type { Key } from 'ink'
import { Box, useInput } from 'ink'
import type { StageKeyHandler } from '../src/commands/accessToken/stageKeyForwardContext.js'
import { SetStageKeyHandlerContext } from '../src/commands/accessToken/stageKeyForwardContext.js'

export function stripAnsi(s: string): string {
  const esc = String.fromCharCode(0x1b)
  return s.replace(new RegExp(`${esc}\\[[0-9;?]*[a-zA-Z]`, 'g'), '')
}

/** Advance the event loop until `predicate` holds or `maxTicks` is exhausted (no fixed wall-clock sleep). */
export async function waitForFrames(
  getCombined: () => string,
  predicate: (combined: string) => boolean,
  maxTicks = 5000
): Promise<void> {
  for (let i = 0; i < maxTicks; i++) {
    if (predicate(getCombined())) {
      return
    }
    await new Promise<void>((resolve) => {
      setImmediate(resolve)
    })
  }
  const combined = getCombined()
  throw new Error(
    `Output condition not met within ${maxTicks} event-loop turns. Last output:\n${combined}`
  )
}

export function waitForLastFrame(
  lastFrame: () => string | undefined,
  predicate: (frame: string) => boolean,
  maxTicks = 5000
): Promise<void> {
  return waitForFrames(() => stripAnsi(lastFrame() ?? ''), predicate, maxTicks)
}

/**
 * `useInput` attaches after `useEffect`; writing to stdin immediately after `render()` can race.
 * Probe with a character, wait until it appears on the command line, then delete it.
 */
export async function renderInkWhenCommandLineReady(element: ReactElement) {
  const result = render(element)
  result.stdin.write('|')
  await waitForLastFrame(result.lastFrame, (f) => f.includes('> |'))
  result.stdin.write('\x7f')
  await waitForLastFrame(
    result.lastFrame,
    (f) => f.includes('> ') && !f.includes('> |')
  )
  return result
}

/** Mirrors InteractiveCliApp stage key forwarding for tests. */
export function StageKeyRoot(props: { readonly children: ReactNode }) {
  const { children } = props
  const stageKeyHandlerRef = useRef<StageKeyHandler | null>(null)
  const setStageKeyHandler = useCallback((handler: StageKeyHandler | null) => {
    stageKeyHandlerRef.current = handler
  }, [])
  useInput(
    useCallback((input: string, key: Key) => {
      stageKeyHandlerRef.current?.(input, key)
    }, [])
  )
  return createElement(
    SetStageKeyHandlerContext.Provider,
    { value: setStageKeyHandler },
    createElement(Box, null, children)
  )
}
