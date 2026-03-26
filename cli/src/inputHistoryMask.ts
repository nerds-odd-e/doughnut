/**
 * Strings stored in past user messages and **user input history** must never
 * include raw secrets for `/add-access-token`.
 */

const ADD_ACCESS_TOKEN = '/add-access-token'

/** Match `/add-access-token` + whitespace + non-empty argument (case-insensitive on the command). */
const ADD_ACCESS_TOKEN_WITH_SECRET = /^\/add-access-token\s+(.+)$/i

/**
 * Returns a display-safe copy of a committed input line before it is stored in past user messages or user input history.
 * Unchanged lines are returned as-is (including original whitespace shape).
 */
export function maskInteractiveInputLineForStorage(line: string): string {
  const segments = line.split('\n')
  let changed = false
  const out = segments.map((segment) => {
    const t = segment.trim()
    if (t.length === 0) return segment
    const m = t.match(ADD_ACCESS_TOKEN_WITH_SECRET)
    if (!m || m[1]!.trim().length === 0) return segment
    changed = true
    return `${ADD_ACCESS_TOKEN} <redacted>`
  })
  return changed ? out.join('\n') : line
}
