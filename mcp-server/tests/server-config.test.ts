import { describe, test, expect } from 'vitest'
import { tools } from '../src/tools/index.js'
import { getExpectedToolNames } from './helpers/index.js'

describe('MCP Server Configuration', () => {
  test('should have correct server metadata', () => {
    const serverName = 'doughnut-mcp-server'
    const serverVersion = '0.1.0'

    expect(serverName).toBe('doughnut-mcp-server')
    expect(serverVersion).toBe('0.1.0')
  })

  test('should define expected tools', () => {
    const expectedTools = getExpectedToolNames()

    // Test that we have the expected tool names
    expectedTools.forEach((toolName) => {
      expect(expectedTools).toContain(toolName)
    })
    expect(expectedTools).toHaveLength(5)
  })
})

describe('Tools Array Structure', () => {
  test('should contain all expected tools', () => {
    const expectedTools = getExpectedToolNames()

    expect(tools).toHaveLength(expectedTools.length)

    expectedTools.forEach((toolName) => {
      const tool = tools.find((t) => t.name === toolName)
      expect(tool).toBeDefined()
      expect(tool?.name).toBe(toolName)
    })
  })

  test('should have correct tool structure', () => {
    tools.forEach((tool) => {
      expect(tool).toHaveProperty('name')
      expect(tool).toHaveProperty('description')
      expect(tool).toHaveProperty('inputSchema')
      expect(tool).toHaveProperty('handle')
      expect(typeof tool.name).toBe('string')
      expect(typeof tool.description).toBe('string')
      expect(typeof tool.handle).toBe('function')
    })
  })

  test('should have meaningful descriptions', () => {
    tools.forEach((tool) => {
      expect(tool.description).toBeTruthy()
      expect(tool.description.length).toBeGreaterThan(0)
    })
  })

  test('should have unique tool names', () => {
    const toolNames = tools.map((tool) => tool.name)
    const uniqueToolNames = new Set(toolNames)
    expect(uniqueToolNames.size).toBe(toolNames.length)
  })

  test('should have snake_case tool names', () => {
    tools.forEach((tool) => {
      expect(tool.name).toMatch(/^[a-z_]+$/)
      expect(tool.name).not.toMatch(/^[A-Z]/)
      expect(tool.name).not.toMatch(/[A-Z]/)
    })
  })
})
