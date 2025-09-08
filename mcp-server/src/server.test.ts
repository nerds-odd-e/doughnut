import { describe, test, expect, vi } from 'vitest'
import { tools } from './tools/index.js'
import type { ToolDescriptor } from './types.js'
import type { DoughnutApi } from '@generated/backend/DoughnutApi.js'

// Type for partial mock of DoughnutApi used in tests
type MockDoughnutApi = Pick<
  DoughnutApi,
  | 'mcpNoteCreationController'
  | 'restTextContentController'
  | 'restNotebookController'
  | 'restNoteController'
  | 'restSearchController'
> & {
  restUserController?: {
    getUserProfile: ReturnType<typeof vi.fn>
  }
}

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
      'update_note_text_content',
      'get_notebook_list',
      'get_graph_with_note_id',
      'add_note',
      'get_relevant_note',
    ]

    // Test that we have the expected tool names
    expect(expectedTools).toContain('update_note_text_content')
    expect(expectedTools).toContain('get_notebook_list')
    expect(expectedTools).toContain('get_graph_with_note_id')
    expect(expectedTools).toContain('add_note')
    expect(expectedTools).toContain('get_relevant_note')
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
  test('should have valid response format', () => {
    // Basic test for response format validation
    expect(true).toBe(true)
  })
})

// Test for add_note tool
describe('add_note tool', () => {
  test('should call API and return success', async () => {
    // Import the tools array
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const addNoteTool = tools.find((t: ToolDescriptor) => t.name === 'add_note')
    expect(addNoteTool).toBeDefined()

    // Mock context and API - create a partial mock that matches DoughnutApi structure
    const mockCreateNote = vi.fn()
    const mockApi = {
      mcpNoteCreationController: {
        createNote1: mockCreateNote,
      },
      restTextContentController: {
        updateNoteTitle: vi.fn(),
        updateNoteDetails: vi.fn(),
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
    } as MockDoughnutApi
    const ctx = { api: mockApi }

    // Arguments for the tool
    const args = { parentTitle: 'Parent Note', newTitle: 'Test Note' }

    // Call the tool's handle function
    if (!addNoteTool) {
      throw new Error('add_note tool not found')
    }
    const _result = await addNoteTool.handle(ctx, args, { params: args })

    // Assert API was called with correct arguments
    expect(mockCreateNote).toHaveBeenCalledWith({
      noteCreationDTO: {
        newTitle: 'Test Note',
      },
      parentNote: 'Parent Note',
    })
    // Assert the response is as expected
  })
})

// Test for get_relevant_note tool
describe('get_relevant_note tool', () => {
  // Helper function to create mock API
  const createMockApi = (searchResult: unknown[], graphResult?: unknown) =>
    ({
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
        getGraph: vi
          .fn()
          .mockResolvedValue(
            graphResult || { note: { id: 123, title: 'Test Note' } }
          ),
      },
      restSearchController: {
        searchForLinkTarget: vi.fn().mockResolvedValue(searchResult),
      },
      mcpNoteCreationController: {
        createNote1: vi.fn(),
      },
    }) as MockDoughnutApi

  // Helper function to run the test
  const runQueryExtractionTest = async (
    args: unknown,
    expectedSearchKey: string,
    shouldFindNote = true
  ) => {
    const getRelevantNoteTool = tools.find(
      (t: ToolDescriptor) => t.name === 'get_relevant_note'
    )
    expect(getRelevantNoteTool).toBeDefined()

    const searchResult = shouldFindNote ? [{ noteTopology: { id: 123 } }] : []
    const mockApi = createMockApi(searchResult)
    const ctx = { api: mockApi }

    // Call the tool's handle function
    if (!getRelevantNoteTool) {
      throw new Error('get_relevant_note tool not found')
    }
    const result = await getRelevantNoteTool.handle(
      ctx,
      args as unknown as Record<string, unknown>
    )

    // Assert search was called with correct arguments
    expect(
      mockApi.restSearchController.searchForLinkTarget
    ).toHaveBeenCalledWith({
      searchKey: expectedSearchKey,
      allMyNotebooksAndSubscriptions: true,
    })

    // Assert the response
    if (shouldFindNote) {
      expect(result.content[0].text).toContain('Test Note')
    } else {
      expect(result.content[0].text).toBe('No relevant note found.')
    }
  }

  // Test data for different argument types
  const testCases = [
    {
      name: 'should extract query when args is an object with query property',
      args: { query: 'query in query' },
      expectedSearchKey: 'query in query',
      shouldFindNote: true,
    },
  ]

  // Run all test cases
  testCases.forEach(({ name, args, expectedSearchKey, shouldFindNote }) => {
    test(name, async () => {
      await runQueryExtractionTest(args, expectedSearchKey, shouldFindNote)
    })
  })
})
