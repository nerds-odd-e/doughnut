import { z } from 'zod'
import type { ToolDescriptor, ServerContext } from '../types.js'
import { createToolHandler, extractParams } from '../utils.js'

/**
 * A tool builder that co-locates schema definition, validation, and handler logic
 * This eliminates the "shotgun surgery" problem by keeping related concerns together
 */
export class ToolBuilder<TSchema extends z.ZodSchema> {
  private name: string
  private description: string
  private schema: TSchema
  private handler?: (
    ctx: ServerContext,
    params: z.infer<TSchema>,
    request?: unknown
  ) => Promise<import('../types.js').ToolResponse>

  constructor(name: string, description: string, schema: TSchema) {
    this.name = name
    this.description = description
    this.schema = schema
  }

  /**
   * Set the handler function for this tool
   */
  handle(
    handler: (
      ctx: ServerContext,
      params: z.infer<TSchema>,
      request?: unknown
    ) => Promise<import('../types.js').ToolResponse>
  ): this {
    this.handler = handler
    return this
  }

  /**
   * Build the final tool descriptor
   */
  build(): ToolDescriptor {
    if (!this.handler) {
      throw new Error(`Handler not set for tool: ${this.name}`)
    }

    return {
      name: this.name,
      description: this.description,
      inputSchema: z.toJSONSchema(this.schema) as Record<string, unknown>,
      handle: createToolHandler(async (ctx, args, request) => {
        const validation = extractParams(args, this.schema)
        if (!validation.success) {
          return {
            content: [
              {
                type: 'text',
                text: `ERROR: ${validation.error}`,
              },
            ],
          }
        }

        return await this.handler!(
          ctx,
          validation.data as z.infer<TSchema>,
          request
        )
      }),
    }
  }
}

/**
 * Helper function to create a new tool builder
 */
export function createTool<TSchema extends z.ZodSchema>(
  name: string,
  description: string,
  schema: TSchema
): ToolBuilder<TSchema> {
  return new ToolBuilder(name, description, schema)
}
