import { OpenAPI } from '@generated/backend'
import { getEnvironmentConfig } from './helpers.js'
import type { ServerContext } from './types.js'

export function createServerContext(): ServerContext {
  const env = getEnvironmentConfig()

  // Configure OpenAPI for generated services
  OpenAPI.BASE = env.apiBaseUrl
  OpenAPI.TOKEN = env.authToken

  return { apiBaseUrl: env.apiBaseUrl, authToken: env.authToken }
}
