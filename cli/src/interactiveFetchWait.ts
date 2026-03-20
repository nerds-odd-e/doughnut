import type { OutputAdapter } from './types.js'

/**
 * User-visible first line of the Current prompt while the TTY awaits a slow
 * network/backend call. Ellipsis animation is applied by the adapter.
 */
export const INTERACTIVE_FETCH_WAIT_LINES = {
  recallNext: 'Loading recall questions',
  contest: 'Regenerating question',
  recallStatus: 'Loading recall status',
  addAccessToken: 'Adding access token',
  createAccessToken: 'Creating access token',
  removeAccessTokenCompletely: 'Removing access token',
  addGmail: 'Connecting Gmail',
  lastEmail: 'Loading last email',
} as const

export type InteractiveFetchWaitLine =
  (typeof INTERACTIVE_FETCH_WAIT_LINES)[keyof typeof INTERACTIVE_FETCH_WAIT_LINES]

let activeWaitLine: InteractiveFetchWaitLine | null = null
let activeWait: { output: OutputAdapter; controller: AbortController } | null =
  null

export function getInteractiveFetchWaitLine(): InteractiveFetchWaitLine | null {
  return activeWaitLine
}

/** Aborts in-flight work for this output only (avoids cross-talk with parallel sessions or tests). */
export function abortInteractiveFetchWait(output: OutputAdapter): void {
  if (!activeWait || activeWait.output !== output) return
  activeWait.controller.abort()
}

function setActiveWaitLine(
  output: OutputAdapter,
  line: InteractiveFetchWaitLine | null
): void {
  activeWaitLine = line
  output.onInteractiveFetchWaitChanged?.()
}

/** Shows grey disabled live region + animated wait line for the duration of `fn`. */
export async function runInteractiveFetchWait<T>(
  output: OutputAdapter,
  line: InteractiveFetchWaitLine,
  fn: (signal: AbortSignal) => Promise<T>
): Promise<T> {
  const ac = new AbortController()
  activeWait = { output, controller: ac }
  setActiveWaitLine(output, line)
  try {
    return await fn(ac.signal)
  } finally {
    if (activeWait?.output === output && activeWait.controller === ac) {
      activeWait = null
    }
    setActiveWaitLine(output, null)
  }
}

export function resetInteractiveFetchWaitForTesting(): void {
  activeWaitLine = null
  activeWait = null
}
