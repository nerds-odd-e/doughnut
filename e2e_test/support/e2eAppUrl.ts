import { E2E_APP_BASE_URL } from '../config/constants'

/** Same origin as Cypress `baseUrl` (app proxy or Vite), for `cy.request` / CLI env. */
export function e2eAppBaseUrl(): string {
  const cfg = Cypress.config() as {
    e2eAppBaseUrl?: string
    baseUrl?: string
  }
  return cfg.e2eAppBaseUrl ?? cfg.baseUrl ?? E2E_APP_BASE_URL
}
