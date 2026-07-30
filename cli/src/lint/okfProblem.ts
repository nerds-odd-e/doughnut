export type OkfProblem = {
  /** What OKF requires is an error; what it recommends is a warning. */
  readonly severity: 'error' | 'warning'
  readonly message: string
}

export function error(message: string): OkfProblem[] {
  return [{ severity: 'error', message }]
}
