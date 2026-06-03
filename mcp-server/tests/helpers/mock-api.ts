import { client } from '@generated/doughnut-backend-api/client.gen'
import type { ServerContext } from '../../src/types.js'

export function setupMockApiClient(): void {
  client.setConfig({
    baseUrl: 'http://localhost:8080',
    headers: {
      Authorization: 'Bearer test-token',
    },
  })
}

// Helper function to create mock context
// Since we now use generated services directly, we just need to provide the context
// The services will use the OpenAPI configuration which can be set up in tests
export function createMockContext(
  overrides: Partial<ServerContext> = {}
): ServerContext {
  return {
    apiBaseUrl: 'http://localhost:8080',
    authToken: 'test-token',
    ...overrides,
  }
}
