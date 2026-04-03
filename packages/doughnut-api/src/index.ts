import { client } from '@generated/doughnut-backend-api/client.gen'
import {
  MemoryTrackerController,
  NotebookController,
  RecallsController,
  RecallPromptController,
  UserController,
} from '@generated/doughnut-backend-api/sdk.gen'
import type {
  Answer,
  AnswerDto,
  AnswerSpellingDto,
  DueMemoryTrackers,
  GeneratedTokenDto,
  MemoryTracker,
  MemoryTrackerLite,
  MultipleChoicesQuestion,
  Note,
  Notebook,
  NotebooksViewedByUser,
  QuestionContestResult,
  RecallPrompt,
  SpellingQuestion,
  TokenConfigDto,
  UserToken,
} from '@generated/doughnut-backend-api'

let sdkHttpStatusOnErrorInterceptorInstalled = false

/** Non-OK responses throw the parsed body only; merge `response.status` so callers can classify 401/403. */
function ensureSdkErrorsIncludeHttpStatus(): void {
  if (sdkHttpStatusOnErrorInterceptorInstalled) return
  sdkHttpStatusOnErrorInterceptorInstalled = true
  client.interceptors.error.use((error: unknown, response: Response) => {
    const status = response?.status
    if (typeof status !== 'number' || !Number.isFinite(status)) {
      return error
    }
    if (error instanceof Error) {
      return error
    }
    if (typeof error === 'object' && error !== null && 'status' in error) {
      return error
    }
    if (typeof error === 'object' && error !== null) {
      return { ...(error as Record<string, unknown>), status }
    }
    return { body: error, status }
  })
}

export function getApiConfig() {
  return {
    apiBaseUrl:
      process.env.DOUGHNUT_API_BASE_URL || 'https://doughnut.odd-e.com',
    authToken: process.env.DOUGHNUT_API_AUTH_TOKEN,
  }
}

export function configureClient(baseUrl: string, authToken?: string): void {
  ensureSdkErrorsIncludeHttpStatus()
  client.setConfig({
    baseUrl,
    headers: authToken ? { Authorization: `Bearer ${authToken}` } : {},
  })
}

export {
  MemoryTrackerController,
  NotebookController,
  RecallsController,
  RecallPromptController,
  UserController,
}
export type {
  Answer,
  AnswerDto,
  AnswerSpellingDto,
  DueMemoryTrackers,
  GeneratedTokenDto,
  MemoryTracker,
  MemoryTrackerLite,
  MultipleChoicesQuestion,
  Note,
  Notebook,
  NotebooksViewedByUser,
  QuestionContestResult,
  RecallPrompt,
  SpellingQuestion,
  TokenConfigDto,
  UserToken,
}
export type { RequestOptions } from '@generated/doughnut-backend-api/client/types.gen'
