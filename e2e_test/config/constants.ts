/**
 * Cypress browser origin for CI / `pnpm test` (phase 6): prod-style single host; static from disk, API via proxy to Spring.
 * Spring Boot E2E still listens on 9081; see e2e_test/e2e-prod-topology-proxy.mjs.
 */
export const E2E_BACKEND_BASE_URL = 'http://localhost:5173'

/** Spring backend only (plugins / tools that must bypass the browser proxy). */
export const E2E_SPRING_BACKEND_URL = 'http://localhost:9081'
