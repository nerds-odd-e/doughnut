import { describe, test, expect } from 'vitest'
import { tools } from '../src/tools/index.js'

// Integration tests for the MCP server
describe('MCP Server Integration', () => {
  test('should load all tools successfully', () => {
    expect(tools).toBeDefined()
    expect(tools.length).toBeGreaterThan(0)
  })

  test('should have all tools properly initialized', () => {
    tools.forEach((tool) => {
      expect(tool.name).toBeDefined()
      expect(tool.description).toBeDefined()
      expect(tool.inputSchema).toBeDefined()
      expect(tool.handle).toBeDefined()
    })
  })
})
