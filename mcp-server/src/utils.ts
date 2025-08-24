import type { ToolResponse } from './types.js'

/**
 * Helper function for error handling
 */
export function createErrorResponse(
  err: unknown,
  prefix = 'ERROR:'
): ToolResponse {
  let msg: string
  if (err instanceof Error) {
    msg = `${prefix} ${err.message}`
  } else if (typeof err === 'string') {
    msg = `${prefix} ${err}`
  } else {
    msg = `${prefix} ${JSON.stringify(err)}`
  }
  return {
    content: [
      {
        type: 'text' as const,
        text: msg,
      },
    ],
  }
}

export function textResponse(message: string): ToolResponse {
  return {
    content: [
      {
        type: 'text' as const,
        text: message,
      },
    ],
  }
}

/**
 * Validate note update parameters
 */
export function validateNoteUpdateParams(
  noteId: number | undefined,
  newTitle: string | null | undefined,
  newDetails: string | null | undefined
): { isValid: boolean; error?: string } {
  if (typeof noteId !== 'number') {
    return { isValid: false, error: 'noteId must be a number' }
  }

  if (typeof newTitle !== 'string' && typeof newDetails !== 'string') {
    return {
      isValid: false,
      error: 'At least one of newTitle or newDetails must be provided.',
    }
  }

  return { isValid: true }
}

/**
 * Extract environment configuration
 */
export function getEnvironmentConfig() {
  return {
    apiBaseUrl: process.env.DOUGHNUT_API_BASE_URL || 'http://localhost:9081',
    authToken: process.env.DOUGHNUT_API_AUTH_TOKEN,
  }
}
