/** CLI environment for E2E: API base URL plus optional overrides. */
import { E2E_BACKEND_BASE_URL } from './constants'

export function cliEnv(overrides?: NodeJS.ProcessEnv): NodeJS.ProcessEnv {
  return {
    DOUGHNUT_API_BASE_URL: E2E_BACKEND_BASE_URL,
    ...overrides,
  }
}
