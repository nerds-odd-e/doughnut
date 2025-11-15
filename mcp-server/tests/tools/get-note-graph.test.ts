import { describe, test, expect, vi } from 'vitest'
import { findTool, createMockApi, createMockContext } from '../helpers/index.js'
import type { RestNoteControllerService } from '@generated/backend'

describe('get_note_graph tool', () => {
  test('should be defined and have correct name', () => {
    const getNoteGraphTool = findTool('get_note_graph')
    expect(getNoteGraphTool).toBeDefined()
    expect(getNoteGraphTool.name).toBe('get_note_graph')
  })

  describe('token limit validation', () => {
    const createNoteGraphMockApi = () =>
      createMockApi({
        restNoteController: {
          getGraph: vi.fn().mockResolvedValue({ focusNote: { id: 1 } }),
        } as unknown as RestNoteControllerService,
      })

    test('should return error when tokenLimit is 0', async () => {
      const getNoteGraphTool = findTool('get_note_graph')
      const mockApi = createNoteGraphMockApi()
      const ctx = createMockContext(mockApi)

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
      const mockApi = createNoteGraphMockApi()
      const ctx = createMockContext(mockApi)

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
      const mockApi = createNoteGraphMockApi()
      const ctx = createMockContext(mockApi)

      const result = await getNoteGraphTool.handle(ctx, {
        noteId: 1,
        tokenLimit: 100,
      })

      expect(mockApi.restNoteController.getGraph).toHaveBeenCalledWith(1, 100)
      expect(result.content[0].text).toContain('"focusNote"')
    })
  })
})
