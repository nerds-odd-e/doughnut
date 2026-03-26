import type { OutputAdapter } from './types.js'

/**
 * Plain-text base for the **Current Stage Indicator** while the TTY awaits a slow
 * network/backend call. TTY animation is `Spinner` in `FetchWaitDisplay.tsx`.
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

/** Esc aborts only the in-flight wait that registered this binding (one TTY `output` at a time). */
type InteractiveFetchWaitEscBinding = {
  output: OutputAdapter
  abortController: AbortController
}
let escBinding: InteractiveFetchWaitEscBinding | null = null

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
 * The same `abortSignal` is aborted if the user presses Esc (TTY: readline `keypress` → `cancelInteractiveFetchWaitFor`).
 */
export type InteractiveFetchWaitTask<T> = (
  abortSignal: AbortSignal
) => Promise<T>

export async function runInteractiveFetchWait<T>(
  output: OutputAdapter,
  line: InteractiveFetchWaitLine,
  task: InteractiveFetchWaitTask<T>
): Promise<T> {
  const abortController = new AbortController()
  escBinding = { output, abortController }
  setActiveWaitLine(output, line)
  try {
    return await task(abortController.signal)
  } finally {
    if (
      escBinding?.output === output &&
      escBinding.abortController === abortController
    ) {
      escBinding = null
    }
    setActiveWaitLine(output, null)
  }
}

/** Esc during an interactive fetch wait: abort in-flight work if this TTY owns it. */
export function cancelInteractiveFetchWaitFor(output: OutputAdapter): boolean {
  if (!escBinding || escBinding.output !== output) {
    return false
  }
  escBinding.abortController.abort()
  return true
}

export function resetInteractiveFetchWaitForTesting(): void {
  activeWaitLine = null
  escBinding = null
}
