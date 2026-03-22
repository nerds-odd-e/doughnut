/**
 * Browser / Cypress entry: fake LB on 5173 (`e2e-prod-topology-proxy.mjs`). `pnpm sut` forwards UI
 * to Vite on 5174; CI/`pnpm test` serves built static from disk. Not the Spring port.
 */
export const E2E_APP_BASE_URL = 'http://localhost:5173'

/** Spring Boot only (direct JVM port; bypasses the app proxy). */
export const E2E_SPRING_BACKEND_URL = 'http://localhost:9081'
