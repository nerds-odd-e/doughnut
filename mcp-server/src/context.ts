import { configureClient, getApiConfig } from 'doughnut-api'
import type { ServerContext } from './types.js'

export function createServerContext(): ServerContext {
  const env = getApiConfig()
  configureClient(env.apiBaseUrl, env.authToken)
  return { apiBaseUrl: env.apiBaseUrl, authToken: env.authToken }
}
