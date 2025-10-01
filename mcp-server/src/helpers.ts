import type { z } from 'zod'
import type { ToolResponse, ServerContext } from './types.js'

/**
 * Helper functions for the MCP server
 */

// Response formatting
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
    content: [{ type: 'text' as const, text: msg }],
  }
}

export function textResponse(message: string): ToolResponse {
  return {
    content: [{ type: 'text' as const, text: message }],
  }
}

export function jsonResponse(data: unknown): ToolResponse {
  return textResponse(JSON.stringify(data))
}

// Environment configuration
export function getEnvironmentConfig() {
  return {
    apiBaseUrl: process.env.DOUGHNUT_API_BASE_URL || 'http://localhost:9081',
    authToken: process.env.DOUGHNUT_API_AUTH_TOKEN,
  }
}

// Parameter validation
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

export function extractNoteId(
  args: Record<string, unknown>,
  request?: unknown
): number | null {
  const argsNoteId = (args as { noteId?: number }).noteId
  if (typeof argsNoteId === 'number') {
    return argsNoteId
  }

  const requestNoteId = (request as { params?: { noteId?: number } })?.params
    ?.noteId
  if (typeof requestNoteId === 'number') {
    return requestNoteId
  }

  return null
}

export function extractTokenLimit(
  args: Record<string, unknown>,
  request?: unknown
): number | null {
  const argsTokenLimit = (args as { tokenLimit?: number }).tokenLimit
  if (typeof argsTokenLimit === 'number') {
    return argsTokenLimit
  }

  const requestTokenLimit = (request as { params?: { tokenLimit?: number } })?.params
    ?.tokenLimit
  if (typeof requestTokenLimit === 'number') {
    return requestTokenLimit
  }

  return null
}

// Tool handler wrapper
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
  return async (ctx, args, request) => {
    try {
      return await handler(ctx, args as T, request)
    } catch (err) {
      return createErrorResponse(err)
    }
  }
}
