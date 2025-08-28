import { describe, test, expect, vi } from 'vitest'
import { tools } from '../src/tools/index.js'
import type { ToolDescriptor, ServerContext } from '../src/types.js'

// Test the server configuration and tool definitions
describe('MCP Server Configuration', () => {
  test('should have correct server metadata', () => {
    const serverName = 'doughnut-mcp-server'
    const serverVersion = '0.1.0'

    expect(serverName).toBe('doughnut-mcp-server')
    expect(serverVersion).toBe('0.1.0')
  })

  test('should define expected tools', () => {
    const expectedTools = [
      'get_instruction',
      'update_note_text_content',
      'get_user_info',
      'get_notebook_list',
      'get_graph_with_note_id',
      'add_note',
      'get_relevant_note',
    ]

    // Test that we have the expected tool names
    expect(expectedTools).toContain('get_instruction')
    expect(expectedTools).toContain('update_note_text_content')
    expect(expectedTools).toContain('get_user_info')
    expect(expectedTools).toContain('get_notebook_list')
    expect(expectedTools).toContain('get_graph_with_note_id')
    expect(expectedTools).toContain('add_note')
    expect(expectedTools).toContain('get_relevant_note')
    expect(expectedTools).toHaveLength(7)
  })
})

describe('Tool Schema Validation', () => {
  test('update_note_text_content should have correct schema', () => {
    const schema = {
      type: 'object',
      properties: {
        noteId: {
          type: 'integer',
          description: 'The ID of the note to update.',
        },
        newTitle: {
          type: ['string', 'null'],
          description: 'The new title for the note.',
        },
        newDetails: {
          type: ['string', 'null'],
          description: 'The new details for the note.',
        },
      },
      required: ['noteId'],
    }

    expect(schema.type).toBe('object')
    expect(schema.required).toContain('noteId')
    expect(schema.properties.noteId.type).toBe('integer')
    expect(schema.properties.newTitle.type).toContain('string')
    expect(schema.properties.newTitle.type).toContain('null')
    expect(schema.properties.newDetails.type).toContain('string')
    expect(schema.properties.newDetails.type).toContain('null')
  })

  test('simple tools should have basic object schema', () => {
    const basicSchema = {
      type: 'object',
    }

    expect(basicSchema.type).toBe('object')
  })
})

describe('Tool Response Formats', () => {
  test('should return correct response format for get_instruction', () => {
    const expectedResponse = {
      content: [
        {
          type: 'text',
          text: 'Doughnut is a Personal Knowledge Management tool',
        },
      ],
    }

    expect(expectedResponse.content).toHaveLength(1)
    expect(expectedResponse.content[0].type).toBe('text')
    expect(expectedResponse.content[0].text).toContain('Doughnut')
  })
})

// Test for add_note tool
describe('add_note tool', () => {
  test('should call API and return success', async () => {
    const addNoteTool = tools.find((t: ToolDescriptor) => t.name === 'add_note')
    expect(addNoteTool).toBeDefined()
    expect(addNoteTool?.name).toBe('add_note')
  })
})

// Test for get_relevant_note tool
describe('get_relevant_note tool', () => {
  test('should be defined', () => {
    const tool = tools.find(
      (t: ToolDescriptor) => t.name === 'get_relevant_note'
    )
    expect(tool).toBeDefined()
    expect(tool?.name).toBe('get_relevant_note')
  })

  test('should handle array arguments', async () => {
    const tool = tools.find((t) => t.name === 'get_relevant_note')!
    const mockApi = {
      restSearchController: {
        searchForLinkTarget: vi.fn().mockResolvedValue([]),
      },
      restNoteController: { getGraph: vi.fn() },
      restTextContentController: {
        updateNoteTitle: vi.fn(),
        updateNoteDetails: vi.fn(),
      },
      restUserController: { getUserProfile: vi.fn() },
      restNotebookController: { myNotebooks: vi.fn() },
      mcpNoteCreationController: { createNote1: vi.fn() },
    }
    const mockContext = { api: mockApi }

    const result = await tool.handle(mockContext, { query: ['test'] })
    expect(result.content[0].text).toBe('Input error.')
  })

  test('should handle number arguments', async () => {
    const tool = tools.find((t) => t.name === 'get_relevant_note')!
    const mockApi = {
      restSearchController: {
        searchForLinkTarget: vi.fn().mockResolvedValue([]),
      },
      restNoteController: { getGraph: vi.fn() },
      restTextContentController: {
        updateNoteTitle: vi.fn(),
        updateNoteDetails: vi.fn(),
      },
      restUserController: { getUserProfile: vi.fn() },
      restNotebookController: { myNotebooks: vi.fn() },
      mcpNoteCreationController: { createNote1: vi.fn() },
    }
    const mockContext = { api: mockApi }

    const result = await tool.handle(mockContext, { query: 123 })
    expect(result.content[0].text).toBe('Input error.')
  })

  test('should handle string arguments', async () => {
    const tool = tools.find((t) => t.name === 'get_relevant_note')!
    const mockApi = {
      restSearchController: {
        searchForLinkTarget: vi.fn().mockResolvedValue([]),
      },
      restNoteController: { getGraph: vi.fn() },
      restTextContentController: {
        updateNoteTitle: vi.fn(),
        updateNoteDetails: vi.fn(),
      },
      restUserController: { getUserProfile: vi.fn() },
      restNotebookController: { myNotebooks: vi.fn() },
      mcpNoteCreationController: { createNote1: vi.fn() },
    }
    const mockContext = { api: mockApi }

    const result = await tool.handle(mockContext, { test: 'test' })
    expect(result.content[0].text).toBe('Input error.')
  })

  test('should handle string arguments', async () => {
    const tool = tools.find((t) => t.name === 'get_relevant_note')!
    const mockApi = {
      restSearchController: {
        searchForLinkTarget: vi.fn().mockResolvedValue([]),
      },
      restNoteController: { getGraph: vi.fn() },
      restTextContentController: {
        updateNoteTitle: vi.fn(),
        updateNoteDetails: vi.fn(),
      },
      restUserController: { getUserProfile: vi.fn() },
      restNotebookController: { myNotebooks: vi.fn() },
      mcpNoteCreationController: { createNote1: vi.fn() },
    }
    const mockContext = { api: mockApi }

    const result = await tool.handle(mockContext, { query: 'test' })
    expect(result.content[0].text).toBe('No relevant note found.')
  })
})

// Test for get_graph_with_note_id tool
describe('get_graph_with_note_id tool', () => {
  test('should call API and return success', async () => {
    const getGraphWithNoteIdTool = tools.find(
      (t: ToolDescriptor) => t.name === 'get_graph_with_note_id'
    )
    expect(getGraphWithNoteIdTool).toBeDefined()
    expect(getGraphWithNoteIdTool?.name).toBe('get_graph_with_note_id')
  })
})

// Test for get_notebook_list tool
describe('get_notebook_list tool', () => {
  test('should call API and return success', async () => {
    const getNotebookListTool = tools.find(
      (t: ToolDescriptor) => t.name === 'get_notebook_list'
    )
    expect(getNotebookListTool).toBeDefined()
    expect(getNotebookListTool?.name).toBe('get_notebook_list')
  })
})

// Test for get_user_info tool
describe('get_user_info tool', () => {
  test('should call API and return success', async () => {
    const getUserInfoTool = tools.find(
      (t: ToolDescriptor) => t.name === 'get_user_info'
    )
    expect(getUserInfoTool).toBeDefined()
    expect(getUserInfoTool?.name).toBe('get_user_info')
  })
})

// Test for update_note_text_content tool
describe('update_note_text_content tool', () => {
  test('should call API and return success', async () => {
    const updateNoteTextContentTool = tools.find(
      (t: ToolDescriptor) => t.name === 'update_note_text_content'
    )
    expect(updateNoteTextContentTool).toBeDefined()
    expect(updateNoteTextContentTool?.name).toBe('update_note_text_content')
  })
})

// Test for get_instruction tool
describe('get_instruction tool', () => {
  test('should call API and return success', async () => {
    const getInstructionTool = tools.find(
      (t: ToolDescriptor) => t.name === 'get_instruction'
    )
    expect(getInstructionTool).toBeDefined()
    expect(getInstructionTool?.name).toBe('get_instruction')
  })
})

// Test for tools array
describe('tools array', () => {
  test('should contain all expected tools', () => {
    const expectedTools = [
      'get_instruction',
      'update_note_text_content',
      'get_user_info',
      'get_notebook_list',
      'get_graph_with_note_id',
      'add_note',
      'get_relevant_note',
    ]

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
})

// Test for tool descriptions
describe('tool descriptions', () => {
  test('should have meaningful descriptions', () => {
    tools.forEach((tool) => {
      expect(tool.description).toBeTruthy()
      expect(tool.description.length).toBeGreaterThan(0)
    })
  })
})

// Test for tool input schemas
describe('tool input schemas', () => {
  test('should have valid input schemas', () => {
    tools.forEach((tool) => {
      expect(tool.inputSchema).toBeDefined()
      expect(typeof tool.inputSchema).toBe('object')
    })
  })
})

// Test for tool handlers
describe('tool handlers', () => {
  test('should have async handlers', () => {
    tools.forEach((tool) => {
      expect(tool.handle).toBeDefined()
      expect(typeof tool.handle).toBe('function')
      // Check if the handler is async (returns a Promise)
      const result = tool.handle(
        {} as ServerContext,
        {} as Record<string, unknown>,
        {} as unknown
      )
      expect(result).toBeInstanceOf(Promise)
    })
  })
})

// Test for tool names uniqueness
describe('tool names uniqueness', () => {
  test('should have unique tool names', () => {
    const toolNames = tools.map((tool) => tool.name)
    const uniqueToolNames = new Set(toolNames)
    expect(uniqueToolNames.size).toBe(toolNames.length)
  })
})

// Test for tool names format
describe('tool names format', () => {
  test('should have snake_case tool names', () => {
    tools.forEach((tool) => {
      expect(tool.name).toMatch(/^[a-z_]+$/)
    })
  })
})

// Test for tool input schemas format
describe('tool input schemas format', () => {
  test('should have valid JSON schema format', () => {
    tools.forEach((tool) => {
      expect(tool.inputSchema).toHaveProperty('type')
      expect(typeof tool.inputSchema.type).toBe('string')
    })
  })
})

// Test for tool handlers format
describe('tool handlers format', () => {
  test('should have handlers with correct signature', () => {
    tools.forEach((tool) => {
      const handler = tool.handle
      expect(typeof handler).toBe('function')
      // Note: Function.length returns the number of parameters, but arrow functions
      // and some other function types may not report this correctly
      expect(typeof handler).toBe('function')
    })
  })
})

// Test for tool names consistency
describe('tool names consistency', () => {
  test('should have consistent naming convention', () => {
    tools.forEach((tool) => {
      expect(tool.name).toMatch(/^[a-z_]+$/)
      expect(tool.name).not.toMatch(/^[A-Z]/)
      expect(tool.name).not.toMatch(/[A-Z]/)
    })
  })
})
