import type { ApiRequestOptions } from '@generated/backend/core/ApiRequestOptions'
import { DoughnutApi } from './DoughnutApiWrapper'

// Global variable to capture request config during extraction
let capturedConfig: ApiRequestOptions | null = null

// Create a capturing HTTP request to extract the request configuration from DoughnutApi
// Uses a build-time transformation (esbuild plugin) to inject a check in the request function
// that looks for a global __extractRequestConfig function
export const extractRequestConfig = (
  serviceMethod: (api: DoughnutApi) => any
): ApiRequestOptions => {
  capturedConfig = null

  // Set up global extractor function that the request function will call
  // (The request function is modified at build time by an esbuild plugin)
  // @ts-ignore - Setting global variable for request extraction
  globalThis.__extractRequestConfig = (options: ApiRequestOptions) => {
    capturedConfig = options
  }

  try {
    const api = new DoughnutApi()
    // Call the service method - this should trigger the request function
    // which will call our global extractor (via the build-time injected check)
    serviceMethod(api)

    if (!capturedConfig) {
      throw new Error('Failed to extract request configuration')
    }

    return capturedConfig
  } finally {
    // Clean up global extractor
    // @ts-ignore
    delete globalThis.__extractRequestConfig
    capturedConfig = null
  }
}
