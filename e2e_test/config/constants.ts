/** Cypress browser origin (fake LB). See docs/gcp/prod_env.md (Local E2E / dev). Not Spring. */
export const E2E_APP_BASE_URL = 'http://localhost:5173'

/** Spring Boot only (direct JVM port; bypasses the app proxy). */
export const E2E_SPRING_BACKEND_URL = 'http://localhost:9081'
