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

type ActiveCancellableWait = {
  output: OutputAdapter
  controller: AbortController
}
let activeCancellableInteractiveWait: ActiveCancellableWait | null = null

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

export type InteractiveFetchWaitFn<T> = (signal: AbortSignal) => Promise<T>

/**
 * Work for one interactive recall load: first `recalling` (and follow-ups) in `recallNext`, tied to
 * {@link runInteractiveRecallLoad} so Esc can abort the same `AbortSignal`.
 */
export type InteractiveRecallLoadFn<T> = InteractiveFetchWaitFn<T>

/**
 * Runs slow interactive work with wait chrome; Esc calls {@link cancelInteractiveFetchWaitFor}
 * to abort the same `AbortSignal` passed to `fn`.
 */
export async function runInteractiveFetchWait<T>(
  output: OutputAdapter,
  line: InteractiveFetchWaitLine,
  fn: InteractiveFetchWaitFn<T>
): Promise<T> {
  const controller = new AbortController()
  activeCancellableInteractiveWait = { output, controller }
  setActiveWaitLine(output, line)
  try {
    return await fn(controller.signal)
  } finally {
    if (
      activeCancellableInteractiveWait?.output === output &&
      activeCancellableInteractiveWait.controller === controller
    ) {
      activeCancellableInteractiveWait = null
    }
    setActiveWaitLine(output, null)
  }
}

export async function runInteractiveRecallLoad<T>(
  output: OutputAdapter,
  load: InteractiveRecallLoadFn<T>
): Promise<T> {
  return runInteractiveFetchWait(
    output,
    INTERACTIVE_FETCH_WAIT_LINES.recallNext,
    load
  )
}

/** Esc during interactive fetch wait: abort in-flight work if this TTY output owns it. */
export function cancelInteractiveFetchWaitFor(output: OutputAdapter): boolean {
  if (
    !activeCancellableInteractiveWait ||
    activeCancellableInteractiveWait.output !== output
  ) {
    return false
  }
  activeCancellableInteractiveWait.controller.abort()
  return true
}

export function resetInteractiveFetchWaitForTesting(): void {
  activeWaitLine = null
  activeCancellableInteractiveWait = null
}
