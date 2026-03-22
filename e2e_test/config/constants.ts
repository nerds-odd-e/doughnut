/**
 * Browser / Cypress entry for E2E: same origin for the UI and `/api` (Vite on `pnpm sut`, or
 * prod-topology proxy on CI — see e2e_test/e2e-prod-topology-proxy.mjs). **127.0.0.1** avoids IPv6
 * / HTTP_PROXY issues on runners; `local.ts` still uses `localhost` for Vite.
 */
export const E2E_APP_BASE_URL = 'http://127.0.0.1:5173'

/** Spring Boot only (direct JVM port; bypasses the app proxy). */
export const E2E_SPRING_BACKEND_URL = 'http://localhost:9081'
