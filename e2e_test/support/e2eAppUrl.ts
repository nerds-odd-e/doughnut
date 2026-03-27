import { E2E_APP_BASE_URL } from '../config/constants'

/** Resolved app origin (`baseUrl` or `E2E_APP_BASE_URL`). See docs/gcp/prod_env.md (Local dev / Cypress). */
export function e2eAppBaseUrl(): string {
  return Cypress.config('baseUrl') ?? E2E_APP_BASE_URL
}
