/** Renders a caught value (typically from a `catch (e)`) as a display-safe message string. */
export function exceptionText(e: unknown): string {
  return e instanceof Error ? e.message : String(e)
}
