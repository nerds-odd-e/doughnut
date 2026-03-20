import type { ChatHistoryOutputTone } from './types.js'

/** Shown in history when the user aborts an interactive network wait (e.g. Esc). */
export const CLI_USER_ABORTED_WAIT_MESSAGE = 'Cancelled by user.' as const

/**
 * Same shape as `fetch` / SDK when `AbortSignal` fires; use for synthetic aborts (CLI delay, tests).
 */
export function userAbortError(): DOMException {
  return new DOMException('The operation was aborted', 'AbortError')
}

/**
 * `fetch` / generated client: user or caller aborted the request (e.g. Esc during recall load).
 * Distinct from “service unavailable” and other network failures.
 */
export function isFetchAbortedByCaller(error: unknown): boolean {
  return (
    (typeof DOMException !== 'undefined' &&
      error instanceof DOMException &&
      error.name === 'AbortError') ||
    (error instanceof Error && error.name === 'AbortError')
  )
}

/** One line + scrollback tone for failures surfaced after a command or fetch-wait (TTY or tests). */
export function userVisibleOutcomeFromCommandError(err: unknown): {
  text: string
  tone: ChatHistoryOutputTone
} {
  if (isFetchAbortedByCaller(err))
    return { text: CLI_USER_ABORTED_WAIT_MESSAGE, tone: 'userNotice' }
  return {
    text: err instanceof Error ? err.message : String(err),
    tone: 'error',
  }
}
