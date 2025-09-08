import { describe, test, expect } from 'vitest'
import { findTool } from '../helpers/index.js'

describe('get_graph_with_note_id tool', () => {
  test('should be defined and have correct name', () => {
    const getGraphWithNoteIdTool = findTool('get_graph_with_note_id')
    expect(getGraphWithNoteIdTool).toBeDefined()
    expect(getGraphWithNoteIdTool.name).toBe('get_graph_with_note_id')
  })
})
