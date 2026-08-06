import { describe, test, expect } from 'vitest'
import { z } from 'zod'
import { createTool } from '../../src/tools/tool-builder.js'

describe('createTool', () => {
  test('emits draft-07 JSON Schema for IDE compatibility', () => {
    const tool = createTool(
      'test_name',
      'test_description',
      z.object({ name: z.string() })
    )
      .handle(async () => ({ content: [] }))
      .build()

    expect((tool.inputSchema as Record<string, unknown>).$schema).toContain(
      'draft-07'
    )
  })
})
