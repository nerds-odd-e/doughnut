import { describe, test, expect } from 'vitest'
import { z } from 'zod'
import { createTool } from '../../src/tools/tool-builder.js'

describe('tool_builder tool', () => {
  test('checks draft-07 in generated schema since VS code does not accept 2020-12 unlike Cursor and IntelliJ, but accepts draft-07', () => {
    const zodSchema = z.object({ name: z.string() })
    const tool = createTool('test_name', 'test_description', zodSchema)
      .handle(async () => {
        // we need a minimal handler to build the tool
        return { content: [] }
      })
      .build()

    const dummySchema = (tool.inputSchema as Record<string, unknown>).$schema
    expect(dummySchema).toContain('draft-07')
  })
})
