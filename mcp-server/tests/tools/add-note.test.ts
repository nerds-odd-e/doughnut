import { describe, test, expect, vi, beforeEach } from 'vitest'
import { createMockContext, findTool } from '../helpers/index.js'
import { McpNoteCreationController } from '@generated/backend/sdk.gen'
import { client } from '@generated/backend/client.gen'
import type { CreateNoteViaMcpResponse } from '@generated/backend'

// Mock the generated services
vi.mock('@generated/backend/sdk.gen', () => ({
  McpNoteCreationController: {
    createNoteViaMcp: vi.fn(),
  },
}))

describe('add_note tool', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Set up OpenAPI config for tests
    client.setConfig({
      baseUrl: 'http://localhost:8080',
      headers: {
        Authorization: 'Bearer test-token',
      },
    })
  })

  test('should call API and return success', async () => {
    const addNoteTool = findTool('add_note')
    expect(addNoteTool).toBeDefined()

    // Mock the service response
    const mockCreateNote = vi.mocked(McpNoteCreationController.createNoteViaMcp)
    const mockResponse: CreateNoteViaMcpResponse = {
      created: {
        note: {
          noteTopology: {
            titleOrPredicate: 'Test Note',
          },
        },
      },
      parent: {
        note: {
          noteTopology: {
            titleOrPredicate: 'Parent Note',
          },
        },
      },
    }
    // OpenAPI client returns { data, error, request, response } structure
    mockCreateNote.mockResolvedValue({
      data: mockResponse,
      error: undefined,
    } as Awaited<ReturnType<typeof McpNoteCreationController.createNoteViaMcp>>)

    const ctx = createMockContext()

    // Arguments for the tool
    const args = { parentTitle: 'Parent Note', newTitle: 'Test Note' }

    // Call the tool's handle function
    const _result = await addNoteTool.handle(ctx, args, { params: args })

    // Assert API was called with correct arguments
    expect(mockCreateNote).toHaveBeenCalledWith({
      body: {
        noteCreationDTO: {
          newTitle: 'Test Note',
        },
        parentNote: 'Parent Note',
      },
    })
  })

  test('should be defined and have correct name', () => {
    const addNoteTool = findTool('add_note')
    expect(addNoteTool.name).toBe('add_note')
  })
})
