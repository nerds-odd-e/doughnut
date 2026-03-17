import { E2E_BACKEND_BASE_URL } from '../config/constants'

/** Backend base URL for E2E: from config or fallback constant. */
export function backendBaseUrl(): string {
  const cfg = Cypress.config() as {
    backendBaseUrl?: string
    baseUrl?: string
  }
  return cfg.backendBaseUrl ?? cfg.baseUrl ?? E2E_BACKEND_BASE_URL
}
