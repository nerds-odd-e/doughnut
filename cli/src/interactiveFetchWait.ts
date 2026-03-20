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

/** Only set while {@link runRecallNextFetchWithWaitUi} is in flight (phase 3.1 — Esc can cancel). */
type InFlightRecallNextFetch = {
  output: OutputAdapter
  controller: AbortController
}
let inFlightRecallNextFetch: InFlightRecallNextFetch | null = null

export function getInteractiveFetchWaitLine(): InteractiveFetchWaitLine | null {
  return activeWaitLine
}

function setActiveWaitLine(
  output: OutputAdapter,
  line: InteractiveFetchWaitLine | null
): void {
  activeWaitLine = line
  output.onInteractiveFetchWaitChanged?.()
}

/**
 * Loads the next recall item from the backend while the TTY shows the loading chrome.
 * The given `signal` is aborted when the user presses Esc (see {@link cancelInFlightRecallNextFetchFor}).
 */
export type RecallNextBackendFetch<T> = (signal: AbortSignal) => Promise<T>

export async function runRecallNextFetchWithWaitUi<T>(
  output: OutputAdapter,
  fetchNextRecallItem: RecallNextBackendFetch<T>
): Promise<T> {
  const controller = new AbortController()
  inFlightRecallNextFetch = { output, controller }
  setActiveWaitLine(output, INTERACTIVE_FETCH_WAIT_LINES.recallNext)
  try {
    return await fetchNextRecallItem(controller.signal)
  } finally {
    if (
      inFlightRecallNextFetch?.output === output &&
      inFlightRecallNextFetch.controller === controller
    ) {
      inFlightRecallNextFetch = null
    }
    setActiveWaitLine(output, null)
  }
}

/** Returns whether a fetch was in flight for this TTY session and was cancelled. */
export function cancelInFlightRecallNextFetchFor(
  output: OutputAdapter
): boolean {
  if (!inFlightRecallNextFetch || inFlightRecallNextFetch.output !== output) {
    return false
  }
  inFlightRecallNextFetch.controller.abort()
  return true
}

/** Non-cancellable waits (token flows, Gmail, contest, etc.). */
export async function runInteractiveFetchWait<T>(
  output: OutputAdapter,
  line: InteractiveFetchWaitLine,
  fn: () => Promise<T>
): Promise<T> {
  setActiveWaitLine(output, line)
  try {
    return await fn()
  } finally {
    setActiveWaitLine(output, null)
  }
}

export function resetInteractiveFetchWaitForTesting(): void {
  activeWaitLine = null
  inFlightRecallNextFetch = null
}
