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

/** Shows grey disabled live region + animated wait line for the duration of `fn`. */
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
}
