/**
 * Single place to turn thrown values into assistant-visible text after slash
 * commands or staged async work. User cancellation (`AbortError`) maps to a
 * fixed phrase; everything else uses `Error#message` or `String(err)`.
 * Scrollback error styling (`PastAssistantErrorBlock`) is separate: it only
 * renders strings already chosen for the transcript.
 */
export function userVisibleSlashCommandError(err: unknown): string {
  if (
    (typeof DOMException !== 'undefined' &&
      err instanceof DOMException &&
      err.name === 'AbortError') ||
    (err instanceof Error && err.name === 'AbortError')
  ) {
    return 'Cancelled.'
  }
  if (err instanceof Error) return err.message
  return String(err)
}
