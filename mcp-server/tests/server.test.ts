import { describe, test, expect } from 'vitest'
import { tools } from '../src/tools/index.js'
import { findTool } from './helpers/index.js'

const EXPECTED_TOOL_NAMES = [
  'get_note_graph',
  'find_most_relevant_note',
] as const

describe('MCP tool registry', () => {
  test('exposes expected tools with required shape', () => {
    expect(tools.map((tool) => tool.name)).toEqual([...EXPECTED_TOOL_NAMES])

    for (const name of EXPECTED_TOOL_NAMES) {
      const tool = findTool(name)
      expect(tool.description).toBeTruthy()
      expect(tool.inputSchema).toBeDefined()
      expect(typeof tool.inputSchema).toBe('object')
      expect(tool.handle).toBeTypeOf('function')
    }
  })
})
