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
