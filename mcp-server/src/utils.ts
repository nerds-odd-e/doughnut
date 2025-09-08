import type { ToolResponse, ServerContext } from './types.js'
import type { DoughnutApi } from '@generated/backend/DoughnutApi.js'
import type { z } from 'zod'

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

/**
 * JSON response helper
 */
export function jsonResponse(data: unknown): ToolResponse {
  return textResponse(JSON.stringify(data))
}

/**
 * Generic tool handler wrapper that provides common functionality
 */
export function createToolHandler<T extends Record<string, unknown>>(
  handler: (
    ctx: ServerContext,
    args: T,
    request?: unknown
  ) => Promise<ToolResponse>
): (
  ctx: ServerContext,
  args: Record<string, unknown>,
  request?: unknown
) => Promise<ToolResponse> {
  return async (
    ctx: ServerContext,
    args: Record<string, unknown>,
    request?: unknown
  ) => {
    try {
      return await handler(ctx, args as T, request)
    } catch (err) {
      return createErrorResponse(err)
    }
  }
}

/**
 * Type-safe parameter extractor using Zod schema
 */
export function extractParams<T>(
  args: Record<string, unknown>,
  schema: z.ZodSchema<T>
): { success: true; data: T } | { success: false; error: string } {
  try {
    const result = schema.safeParse(args)
    if (result.success) {
      return { success: true, data: result.data }
    }
    return { success: false, error: result.error.message }
  } catch (err) {
    return {
      success: false,
      error: err instanceof Error ? err.message : 'Unknown validation error',
    }
  }
}

/**
 * Get note by ID helper
 */
export async function getNoteById(
  api: DoughnutApi,
  noteId: number
): Promise<ToolResponse> {
  const graph = await api.restNoteController.getGraph(noteId)
  return jsonResponse(graph)
}

/**
 * Extract noteId from args with proper validation
 */
export function extractNoteId(
  args: Record<string, unknown>,
  request?: unknown
): number | null {
  // Try args first
  const argsNoteId = (args as { noteId?: number }).noteId
  if (typeof argsNoteId === 'number') {
    return argsNoteId
  }

  // Try request params for compatibility
  const requestNoteId = (request as { params?: { noteId?: number } })?.params
    ?.noteId
  if (typeof requestNoteId === 'number') {
    return requestNoteId
  }

  return null
}

/**
 * Format notebook list response
 */
export function formatNotebookListResponse(
  notebooks: Array<{ title?: string; headNoteId?: number }>
): ToolResponse {
  return {
    content: [
      {
        type: 'text',
        text: JSON.stringify(
          notebooks.map((n) => ({
            title: n.title ?? '',
            headNoteId: n.headNoteId ?? null,
          }))
        ),
      },
    ],
  }
}
