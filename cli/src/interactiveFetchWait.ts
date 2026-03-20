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

type ActiveInteractiveRecallLoad = {
  output: OutputAdapter
  controller: AbortController
}
let activeInteractiveRecallLoad: ActiveInteractiveRecallLoad | null = null

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
 * Work for one interactive recall load: first `recalling` (and follow-ups) in `recallNext`, tied to
 * {@link runInteractiveRecallLoad} so Esc can abort the same `AbortSignal`.
 */
export type InteractiveRecallLoadFn<T> = (
  recallLoadSignal: AbortSignal
) => Promise<T>

export async function runInteractiveRecallLoad<T>(
  output: OutputAdapter,
  load: InteractiveRecallLoadFn<T>
): Promise<T> {
  const controller = new AbortController()
  activeInteractiveRecallLoad = { output, controller }
  setActiveWaitLine(output, INTERACTIVE_FETCH_WAIT_LINES.recallNext)
  try {
    return await load(controller.signal)
  } finally {
    if (
      activeInteractiveRecallLoad?.output === output &&
      activeInteractiveRecallLoad.controller === controller
    ) {
      activeInteractiveRecallLoad = null
    }
    setActiveWaitLine(output, null)
  }
}

/** Esc during recall load: abort in-flight work if this TTY output owns it. */
export function cancelInteractiveRecallLoadFor(output: OutputAdapter): boolean {
  if (
    !activeInteractiveRecallLoad ||
    activeInteractiveRecallLoad.output !== output
  ) {
    return false
  }
  activeInteractiveRecallLoad.controller.abort()
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
  activeInteractiveRecallLoad = null
}
