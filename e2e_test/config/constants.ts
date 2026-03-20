/** E2E backend URL for Cypress and CLI subprocess env. 127.0.0.1 avoids Linux CI resolving `localhost` to ::1 when the server is IPv4-only. */
export const E2E_BACKEND_BASE_URL = 'http://127.0.0.1:9081'
