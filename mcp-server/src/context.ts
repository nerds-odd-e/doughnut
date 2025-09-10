import { getEnvironmentConfig } from './helpers.js'
import { createDoughnutApi } from './api.js'
import type { ServerContext } from './types.js'

export function createServerContext(): ServerContext {
  const env = getEnvironmentConfig()
  const api = createDoughnutApi({
    apiBaseUrl: env.apiBaseUrl,
    authToken: env.authToken,
  })
  return { api, authToken: env.authToken }
}
