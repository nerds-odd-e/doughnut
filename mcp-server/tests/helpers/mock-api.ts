import { DoughnutApi } from '../../src/DoughnutApiExport.js'

// Helper function to create a real API with selective method overrides
export function createMockApi(
  overrides: Partial<DoughnutApi> = {}
): DoughnutApi {
  // Create a real DoughnutApi instance
  const mockApi = new DoughnutApi({
    BASE: 'http://localhost:8080',
    TOKEN: 'test-token',
  })

  // Apply overrides using spread operator
  return { ...mockApi, ...overrides }
}

// Helper function to create mock context
export function createMockContext(api: DoughnutApi) {
  return { api }
}
