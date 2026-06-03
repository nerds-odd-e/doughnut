import { describe, test, expect } from 'vitest'
import { tools } from '../src/tools/index.js'

describe('Tool input schemas', () => {
  test('each tool has JSON object input schema', () => {
    for (const tool of tools) {
      expect(tool.inputSchema).toBeDefined()
      expect(typeof tool.inputSchema).toBe('object')
      expect(tool.inputSchema).toHaveProperty('type')
      expect(typeof tool.inputSchema.type).toBe('string')
    }
  })
})

describe('Tool handlers', () => {
  test('each tool exposes an async handler', () => {
    for (const tool of tools) {
      expect(tool.handle).toBeTypeOf('function')
      expect(tool.handle.constructor.name).toBe('AsyncFunction')
    }
  })
})
