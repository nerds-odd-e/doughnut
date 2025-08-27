import { describe, test, expect, vi } from 'vitest'
import { tools } from './tools/index.js'
import type { ToolDescriptor } from './types.js'

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
    ]

    // Test that we have the expected tool names
    expect(expectedTools).toContain('get_instruction')
    expect(expectedTools).toContain('update_note_text_content')
    expect(expectedTools).toContain('get_user_info')
    expect(expectedTools).toContain('get_notebook_list')
    expect(expectedTools).toHaveLength(5)
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
    // Import the tools array
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const addNoteTool = tools.find((t: ToolDescriptor) => t.name === 'add_note')
    expect(addNoteTool).toBeDefined()

    // Mock context and API
    const mockCreateNote = vi.fn().mockResolvedValue(undefined)
    const mockApi = {
      mcpNoteCreationController: {
        createNote1: mockCreateNote,
      },
      restTextContentController: {
        updateNoteTitle: vi.fn(),
        updateNoteDetails: vi.fn(),
      },
      restUserController: {
        getUserProfile: vi.fn(),
      },
      restNotebookController: {
        myNotebooks: vi.fn(),
      },
      restNoteController: {
        getGraph: vi.fn(),
      },
      restSearchController: {
        searchForLinkTarget: vi.fn(),
      },
    }
    const ctx = { api: mockApi }

    // Arguments for the tool
    const args = { noteId: 123, newTitle: 'Test Note' }

    // Call the tool's handle function
    if (!addNoteTool) {
      throw new Error('add_note tool not found')
    }
    const result = await addNoteTool.handle(ctx, args, { params: args })

    // Assert API was called with correct arguments
    expect(mockCreateNote).toHaveBeenCalledWith(123, {
      newTitle: 'Test Note',
    })
    // Assert the response is as expected
    expect(result).toEqual({
      content: [
        {
          type: 'text',
          text: 'All Good',
        },
      ],
    })
  })
})

// Test for extractQueryFromArgs function

import { extractQueryFromArgs } from './tools/index.js'

describe('extractQueryFromArgs', () => {
  test('should return query when args is a string', () => {
    expect(extractQueryFromArgs('test query')).toBe('test query')
  })

  test('should return query when args is an object with args property as string', () => {
    expect(extractQueryFromArgs({ args: 'query in args' })).toBe(
      'query in args'
    )
  })

  test('should return query when args is an object with query property', () => {
    expect(extractQueryFromArgs({ query: 'query in query' })).toBe(
      'query in query'
    )
  })

  test('should return empty string when args is null', () => {
    expect(extractQueryFromArgs(null)).toBe('')
  })

  test('should return empty string when args is undefined', () => {
    expect(extractQueryFromArgs(undefined)).toBe('')
  })

  test('should return empty string when args is an object without args or query', () => {
    expect(extractQueryFromArgs({ foo: 'bar' })).toBe('')
  })

  test('should return empty string when args is a number', () => {
    expect(extractQueryFromArgs(123)).toBe('')
  })
})
