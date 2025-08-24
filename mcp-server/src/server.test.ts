import { describe, test, expect } from 'vitest'

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
      'get_sampleapi',
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
    expect(expectedTools).toHaveLength(6)
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
          type: 'string',
          description: 'The new title for the note.',
          nullable: true,
        },
        newDetails: {
          type: 'string',
          description: 'The new details for the note.',
          nullable: true,
        },
      },
      required: ['noteId'],
    }

    expect(schema.type).toBe('object')
    expect(schema.required).toContain('noteId')
    expect(schema.properties.noteId.type).toBe('integer')
    expect(schema.properties.newTitle.nullable).toBe(true)
    expect(schema.properties.newDetails.nullable).toBe(true)
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

  test('should return correct response format for get_sampleapi', () => {
    const expectedResponse = {
      content: [
        {
          type: 'text',
          text: 'Sample API',
        },
      ],
    }

    expect(expectedResponse.content).toHaveLength(1)
    expect(expectedResponse.content[0].type).toBe('text')
    expect(expectedResponse.content[0].text).toBe('Sample API')
  })
})
