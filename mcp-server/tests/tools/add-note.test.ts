import { describe, test, expect, vi } from 'vitest'
import { createMockApi, createMockContext, findTool } from '../helpers/index.js'

describe('add_note tool', () => {
  test('should call API and return success', async () => {
    const addNoteTool = findTool('add_note')
    expect(addNoteTool).toBeDefined()

    // Mock only the specific method we need to test
    const mockCreateNote = vi.fn()
    const mockApi = createMockApi({
      mcpNoteCreationController: {
        createNote1: mockCreateNote,
      },
    })
    const ctx = createMockContext(mockApi)

    // Arguments for the tool
    const args = { parentTitle: 'Parent Note', newTitle: 'Test Note' }

    // Call the tool's handle function
    const _result = await addNoteTool.handle(ctx, args, { params: args })

    // Assert API was called with correct arguments
    expect(mockCreateNote).toHaveBeenCalledWith({
      noteCreationDTO: {
        newTitle: 'Test Note',
      },
      parentNote: 'Parent Note',
    })
  })

  test('should be defined and have correct name', () => {
    const addNoteTool = findTool('add_note')
    expect(addNoteTool.name).toBe('add_note')
  })
})
