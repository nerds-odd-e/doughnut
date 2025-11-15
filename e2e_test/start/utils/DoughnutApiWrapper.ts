// Re-export DoughnutApi for e2e tests
// This file allows the e2e test build system to resolve the import
// Note: This uses @frontend alias configured in common.ts (temporary solution)
export { DoughnutApi } from '@frontend/managedApi/DoughnutApi'
