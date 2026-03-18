import { client } from '@generated/doughnut-backend-api/client.gen'
import {
  MemoryTrackerController,
  RecallsController,
  RecallPromptController,
  UserController,
} from '@generated/doughnut-backend-api/sdk.gen'
import type {
  MemoryTrackerLite,
  RecallPrompt,
} from '@generated/doughnut-backend-api'

export function getApiConfig() {
  return {
    apiBaseUrl:
      process.env.DOUGHNUT_API_BASE_URL || 'https://doughnut.odd-e.com',
    authToken: process.env.DOUGHNUT_API_AUTH_TOKEN,
  }
}

export function configureClient(baseUrl: string, authToken?: string): void {
  client.setConfig({
    baseUrl,
    headers: authToken ? { Authorization: `Bearer ${authToken}` } : {},
  })
}

export {
  MemoryTrackerController,
  RecallsController,
  RecallPromptController,
  UserController,
}
export type { MemoryTrackerLite, RecallPrompt }
