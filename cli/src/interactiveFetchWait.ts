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

type EscBoundAbort = {
  output: OutputAdapter
  controller: AbortController
}
let escBoundAbort: EscBoundAbort | null = null

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
 * Backend/network work executed while the interactive fetch-wait chrome is shown.
 * The same `abortSignal` is aborted if the user presses Esc on this `output`.
 */
export type InteractiveFetchWaitTask<T> = (
  abortSignal: AbortSignal
) => Promise<T>

export async function runInteractiveFetchWait<T>(
  output: OutputAdapter,
  line: InteractiveFetchWaitLine,
  task: InteractiveFetchWaitTask<T>
): Promise<T> {
  const controller = new AbortController()
  escBoundAbort = { output, controller }
  setActiveWaitLine(output, line)
  try {
    return await task(controller.signal)
  } finally {
    if (
      escBoundAbort?.output === output &&
      escBoundAbort.controller === controller
    ) {
      escBoundAbort = null
    }
    setActiveWaitLine(output, null)
  }
}

/** Esc during an interactive fetch wait: abort in-flight work if this TTY owns it. */
export function cancelInteractiveFetchWaitFor(output: OutputAdapter): boolean {
  if (!escBoundAbort || escBoundAbort.output !== output) {
    return false
  }
  escBoundAbort.controller.abort()
  return true
}

export function resetInteractiveFetchWaitForTesting(): void {
  activeWaitLine = null
  escBoundAbort = null
}
