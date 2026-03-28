import type { CliAssistantMessageTone } from './types.js'

/** Shown in past CLI assistant messages when the user aborts an interactive network wait (e.g. Esc). */
export const CLI_USER_ABORTED_WAIT_MESSAGE = 'Cancelled by user.' as const

/**
 * Same shape as `fetch` / SDK when `AbortSignal` fires; use for synthetic aborts (CLI delay, tests).
 */
export function userAbortError(): DOMException {
  return new DOMException('The operation was aborted', 'AbortError')
}

/**
 * `fetch` / generated client: user or caller aborted the request (e.g. Esc during fetch-wait).
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

/** One line + CLI assistant message tone for failures surfaced after a command or fetch-wait (TTY or tests). */
export function userVisibleOutcomeFromCommandError(err: unknown): {
  text: string
  tone: CliAssistantMessageTone
} {
  if (isFetchAbortedByCaller(err))
    return { text: CLI_USER_ABORTED_WAIT_MESSAGE, tone: 'userNotice' }
  return {
    text: err instanceof Error ? err.message : String(err),
    tone: 'error',
  }
}
