import { client } from '@generated/backend/client.gen'
import { getEnvironmentConfig } from './helpers.js'
import type { ServerContext } from './types.js'

export function createServerContext(): ServerContext {
  const env = getEnvironmentConfig()

  // Configure client for generated services
  client.setConfig({
    baseUrl: env.apiBaseUrl,
    headers: {
      Authorization: `Bearer ${env.authToken}`,
    },
  })

  return { apiBaseUrl: env.apiBaseUrl, authToken: env.authToken }
}
