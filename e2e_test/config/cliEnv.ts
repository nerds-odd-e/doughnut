/** CLI environment for E2E: API base URL plus optional overrides. */
import { E2E_APP_BASE_URL } from './constants'

export function cliEnv(overrides?: NodeJS.ProcessEnv): NodeJS.ProcessEnv {
  return {
    DOUGHNUT_API_BASE_URL: E2E_APP_BASE_URL,
    // Override CI=1 (set by nix dev env) so Ink uses non-CI rendering mode,
    // which writes interactive UI output to stdout for E2E assertions.
    CI: '0',
    // Chalk/Ink gray backgrounds (e.g. \x1b[100m) for past user rows in PTY captures.
    FORCE_COLOR: '1',
    ...overrides,
  }
}
