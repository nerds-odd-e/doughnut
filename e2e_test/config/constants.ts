/**
 * Browser / Cypress entry for E2E: same origin for the UI and `/api` (Vite on `pnpm sut`, or
 * prod-topology proxy on CI — see e2e_test/e2e-prod-topology-proxy.mjs). Not the Spring port.
 */
export const E2E_APP_BASE_URL = 'http://localhost:5173'

/** Spring Boot only (direct JVM port; bypasses the app proxy). */
export const E2E_SPRING_BACKEND_URL = 'http://localhost:9081'
