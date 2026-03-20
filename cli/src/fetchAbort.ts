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
