import { describe, test, expect, vi, beforeEach } from 'vitest'
import { createMockContext, findTool } from '../helpers/index.js'
import * as Services from '@generated/backend/sdk.gen'
import { OpenAPI } from '@generated/backend'
import type { CreateNoteViaMcpResponse } from '@generated/backend'

// Mock the generated services
vi.mock('@generated/backend/sdk.gen', () => ({
  createNoteViaMcp: vi.fn(),
}))

describe('add_note tool', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Set up OpenAPI config for tests
    OpenAPI.BASE = 'http://localhost:8080'
    OpenAPI.TOKEN = 'test-token'
  })

  test('should call API and return success', async () => {
    const addNoteTool = findTool('add_note')
    expect(addNoteTool).toBeDefined()

    // Mock the service response
    const mockCreateNote = vi.mocked(Services.createNoteViaMcp)
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
    mockCreateNote.mockResolvedValue(mockResponse)

    const ctx = createMockContext()

    // Arguments for the tool
    const args = { parentTitle: 'Parent Note', newTitle: 'Test Note' }

    // Call the tool's handle function
    const _result = await addNoteTool.handle(ctx, args, { params: args })

    // Assert API was called with correct arguments
    expect(mockCreateNote).toHaveBeenCalledWith({
      requestBody: {
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
