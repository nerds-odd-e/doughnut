import { describe, test, expect, vi, beforeEach } from 'vitest'
import { findTool, createMockContext } from '../helpers/index.js'
import * as Services from '@generated/backend/sdk.gen'
import { client } from '@generated/backend/client.gen'
import type { GetGraphResponse } from '@generated/backend'

// Mock the generated services
vi.mock('@generated/backend/sdk.gen', () => ({
  getGraph: vi.fn(),
}))

describe('get_note_graph tool', () => {
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

  test('should be defined and have correct name', () => {
    const getNoteGraphTool = findTool('get_note_graph')
    expect(getNoteGraphTool).toBeDefined()
    expect(getNoteGraphTool.name).toBe('get_note_graph')
  })

  describe('token limit validation', () => {
    test('should return error when tokenLimit is 0', async () => {
      const getNoteGraphTool = findTool('get_note_graph')
      const ctx = createMockContext()

      const result = await getNoteGraphTool.handle(ctx, {
        noteId: 1,
        tokenLimit: 0,
      })

      expect(result.content[0].text).toBe(
        'ERROR: tokenLimit must be a positive number'
      )
    })

    test('should return error when tokenLimit is too low (<=5)', async () => {
      const getNoteGraphTool = findTool('get_note_graph')
      const ctx = createMockContext()

      const result = await getNoteGraphTool.handle(ctx, {
        noteId: 1,
        tokenLimit: 5,
      })

      expect(result.content[0].text).toBe(
        'ERROR: tokenLimit too low to fetch any note'
      )
    })

    test('should successfully fetch graph with valid tokenLimit', async () => {
      const getNoteGraphTool = findTool('get_note_graph')
      const ctx = createMockContext()

      // Mock the service response
      const mockGetGraph = vi.mocked(Services.getGraph)
      const mockResponse: GetGraphResponse = {
        focusNote: { id: 1 },
      } as GetGraphResponse
      // OpenAPI client returns { data, error, request, response } structure
      mockGetGraph.mockResolvedValue({
        data: mockResponse,
        error: undefined,
      } as Awaited<ReturnType<typeof Services.getGraph>>)

      const result = await getNoteGraphTool.handle(ctx, {
        noteId: 1,
        tokenLimit: 100,
      })

      expect(mockGetGraph).toHaveBeenCalledWith({
        path: { note: 1 },
        query: { tokenLimit: 100 },
      })
      expect(result.content[0].text).toContain('"focusNote"')
    })
  })
})
