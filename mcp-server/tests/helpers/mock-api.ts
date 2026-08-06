import type { ServerContext } from '../../src/types.js'

export function createMockContext(
  overrides: Partial<ServerContext> = {}
): ServerContext {
  return {
    apiBaseUrl: 'http://localhost:8080',
    authToken: 'test-token',
    ...overrides,
  }
}
