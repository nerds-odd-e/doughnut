import { describe, test, expect, vi, beforeEach } from 'vitest'
import {
  findTool,
  createMockContext,
  setupMockApiClient,
} from '../helpers/index.js'
import { NoteController } from '@generated/doughnut-backend-api/sdk.gen'
import type { GetGraphResponse } from '@generated/doughnut-backend-api'

vi.mock('@generated/doughnut-backend-api/sdk.gen', () => ({
  NoteController: {
    getGraph: vi.fn(),
  },
}))

describe('get_note_graph tool', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setupMockApiClient()
  })

  describe('token limit validation', () => {
    test.each([
      [0, 'ERROR: tokenLimit must be a positive number'],
      [5, 'ERROR: tokenLimit too low to fetch any note'],
    ])('returns error when tokenLimit is %i', async (tokenLimit, expectedError) => {
      const tool = findTool('get_note_graph')
      const result = await tool.handle(createMockContext(), {
        noteId: 1,
        tokenLimit,
      })
      expect(result.content[0].text).toBe(expectedError)
    })

    test('should successfully fetch graph with valid tokenLimit', async () => {
      const tool = findTool('get_note_graph')
      const mockGetGraph = vi.mocked(NoteController.getGraph)
      const mockResponse: GetGraphResponse = {
        focusNote: {
          title: 'Example',
          depth: 0,
          outgoingLinks: [],
          inboundReferences: [],
          sampleSiblings: [],
          content: '',
          contentTruncated: false,
        },
        relatedNotes: [],
      }
      mockGetGraph.mockResolvedValue({
        data: mockResponse,
        error: undefined,
      } as Awaited<ReturnType<typeof NoteController.getGraph>>)

      const result = await tool.handle(createMockContext(), {
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
