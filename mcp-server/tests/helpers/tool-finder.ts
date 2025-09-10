import { tools } from '../../src/tools/index.js'
import type { ToolDescriptor } from '../../src/types.js'

// Helper function to find a tool by name
export function findTool(toolName: string): ToolDescriptor {
  const tool = tools.find((t: ToolDescriptor) => t.name === toolName)
  if (!tool) {
    throw new Error(`${toolName} tool not found`)
  }
  return tool
}

// Helper function to get all expected tool names
export function getExpectedToolNames(): string[] {
  return [
    'get_notebook_list',
    'get_graph_with_note_id',
    'add_note',
    'get_relevant_note',
  ]
}
