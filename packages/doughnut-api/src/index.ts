import { client } from '@generated/backend/client.gen'

export function getApiConfig() {
  return {
    apiBaseUrl: process.env.DOUGHNUT_API_BASE_URL || 'http://localhost:9081',
    authToken: process.env.DOUGHNUT_API_AUTH_TOKEN,
  }
}

export function configureClient(baseUrl: string, authToken?: string): void {
  client.setConfig({
    baseUrl,
    headers: authToken ? { Authorization: `Bearer ${authToken}` } : {},
  })
}
