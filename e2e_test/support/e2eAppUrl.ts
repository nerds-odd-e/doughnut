import { E2E_APP_BASE_URL } from '../config/constants'

/** Cypress `baseUrl` (Vite or prod-topology proxy); used for `cy.request` / CLI env. */
export function e2eAppBaseUrl(): string {
  return Cypress.config('baseUrl') ?? E2E_APP_BASE_URL
}
