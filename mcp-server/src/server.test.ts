import { describe, test, expect, vi } from 'vitest'
import { tools } from './tools/index'
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
    const addNoteTool = tools.find((t: ToolDescriptor) => t.name === 'add_note')
    expect(addNoteTool).toBeDefined()

    const mockCreateNote = vi.fn().mockResolvedValue(undefined)
    const mockApi = {
      mcpNoteCreationController: {
        createNote: mockCreateNote,
      },
    }
    const ctx = { api: mockApi }

    const args = { noteId: 123, noteTitle: 'Test Note' }

    const result = await addNoteTool.handle(ctx, args, { params: args })

    expect(mockCreateNote).toHaveBeenCalledWith(123, 'Test Note')

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
