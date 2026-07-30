export type OkfProblem = {
  /** What OKF requires is an error; what it recommends is a warning. */
  readonly severity: 'error' | 'warning'
  /** The 1-based line to send the reader to; absent when the file itself is the problem. */
  readonly line?: number
  readonly message: string
}

/** Line 1 by default: a frontmatter problem is about the block a file opens with. */
export function error(message: string, line = 1): OkfProblem[] {
  return [{ severity: 'error', line, message }]
}
