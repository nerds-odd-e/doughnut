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
import { vi } from 'vitest'
import type { StageKeyHandler } from '../src/commonUIComponents/stageKeyForwardContext.js'
import { SetStageKeyHandlerContext } from '../src/commonUIComponents/stageKeyForwardContext.js'

export function stripAnsi(s: string): string {
  const esc = String.fromCharCode(0x1b)
  return s.replace(new RegExp(`${esc}\\[[0-9;?]*[a-zA-Z]`, 'g'), '')
}

/** Raw TTY ESC byte for Ink `useInput` and interactive tests. */
export const TTY_ESCAPE = '\u001b'

export function pressEscape(stdin: { write(data: string): void }): void {
  stdin.write(TTY_ESCAPE)
}

/**
 * Ink 7 buffers a lone ESC for ~20ms before emitting it (`pendingInputFlushDelayMilliseconds`).
 * `waitForFrames` spins `setImmediate` which does not advance wall-clock time, so on fast CI
 * all turns can finish before the timer fires. We install fake timers around the ESC + wait
 * to guarantee the deferred flush runs.
 */
async function withDeferredEscapeFlush(fn: () => Promise<void>): Promise<void> {
  const alreadyFake = vi.isFakeTimers()
  if (!alreadyFake) {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] })
  }
  try {
    await fn()
  } finally {
    if (!alreadyFake) {
      vi.useRealTimers()
    }
  }
}

/** Send Esc, then poll until `predicate(combined)` holds. */
export async function pressEscapeAndWait(
  stdin: { write(data: string): void },
  getCombined: () => string,
  predicate: (combined: string) => boolean,
  maxTicks = 5000
): Promise<void> {
  await withDeferredEscapeFlush(async () => {
    pressEscape(stdin)
    await vi.advanceTimersByTimeAsync(25)
    await waitForFrames(getCombined, predicate, maxTicks)
  })
}

/** Poll until normalized output contains the cancelled assistant line. */
export async function waitForCancelledLine(
  getCombined: () => string,
  options?: { readonly normalize?: (s: string) => string },
  maxTicks = 5000
): Promise<void> {
  const norm = options?.normalize ?? ((s: string) => s)
  await waitForFrames(
    getCombined,
    (c) => norm(c).includes('Cancelled.'),
    maxTicks
  )
}

/** Send Esc, then wait until output shows `Cancelled.` (after optional normalization). */
export async function pressEscapeAndWaitForCancelledLine(
  stdin: { write(data: string): void },
  getCombined: () => string,
  options?: { readonly normalize?: (s: string) => string },
  maxTicks = 5000
): Promise<void> {
  await withDeferredEscapeFlush(async () => {
    pressEscape(stdin)
    await vi.advanceTimersByTimeAsync(25)
    await waitForCancelledLine(getCombined, options, maxTicks)
  })
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

function strippedOutputMatches(
  haystack: string,
  pattern: string | RegExp
): boolean {
  return typeof pattern === 'string'
    ? haystack.includes(pattern)
    : pattern.test(haystack)
}

export type InkTestRenderResult = ReturnType<typeof render>

export async function waitUntilInkLastFrameStripped(
  lastFrame: () => string | undefined,
  predicate: (stripped: string) => boolean,
  maxTicks = 5000
): Promise<void> {
  return waitForFrames(() => stripAnsi(lastFrame() ?? ''), predicate, maxTicks)
}

/**
 * `useInput` attaches after `useEffect`; writing to stdin immediately after `render()` can race.
 * Write a probe character, wait until the stripped last frame matches `probeVisible`, backspace, then wait for `probeHidden`.
 */
export async function inkCommandLineProbeUndelete(
  result: InkTestRenderResult,
  options: {
    readonly probeChar: string
    readonly probeVisible: (strippedLastFrame: string) => boolean
    readonly probeHidden: (strippedLastFrame: string) => boolean
  },
  maxTicks = 5000
): Promise<void> {
  const { lastFrame, stdin } = result
  const { probeChar, probeVisible, probeHidden } = options
  stdin.write(probeChar)
  await waitUntilInkLastFrameStripped(lastFrame, probeVisible, maxTicks)
  stdin.write('\x7f')
  await waitUntilInkLastFrameStripped(lastFrame, probeHidden, maxTicks)
}

/** Shared wait helpers for Ink interactive tests (last frame / full scrollback). */
export function extendInkRenderForInteractiveTests(
  result: InkTestRenderResult
) {
  const { frames, lastFrame } = result
  return {
    lastStrippedFrame(): string {
      return stripAnsi(lastFrame() ?? '')
    },
    waitForLastFrameToInclude(
      pattern: string | RegExp,
      maxTicks = 5000
    ): Promise<void> {
      return waitUntilInkLastFrameStripped(
        lastFrame,
        (f) => strippedOutputMatches(f, pattern),
        maxTicks
      )
    },
    waitForFramesToInclude(
      pattern: string | RegExp,
      maxTicks = 5000
    ): Promise<void> {
      return waitForFrames(
        () => stripAnsi(frames.join('\n')),
        (c) => strippedOutputMatches(c, pattern),
        maxTicks
      )
    },
    waitUntilLastFrame(
      predicate: (stripped: string) => boolean,
      maxTicks = 5000
    ): Promise<void> {
      return waitUntilInkLastFrameStripped(lastFrame, predicate, maxTicks)
    },
    waitForLastFrameRaw(
      predicate: (raw: string) => boolean,
      maxTicks = 5000
    ): Promise<void> {
      return waitForFrames(() => lastFrame() ?? '', predicate, maxTicks)
    },
  }
}

/**
 * `useInput` attaches after `useEffect`; writing to stdin immediately after `render()` can race.
 * Probe with `|`, wait until it appears on the command line, then delete it.
 */
export async function renderInkWhenCommandLineReady(element: ReactElement) {
  const result = render(element)
  await inkCommandLineProbeUndelete(result, {
    probeChar: '|',
    probeVisible: (f) => f.includes('→ |') || f.includes('> |'),
    probeHidden: (f) =>
      (f.includes('→') && !f.includes('→ |')) ||
      (f.includes('>') && !f.includes('> |')),
  })
  return { ...result, ...extendInkRenderForInteractiveTests(result) }
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
