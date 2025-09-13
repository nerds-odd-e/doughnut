import { describe, test, expect } from 'vitest'
import { findTool } from '../helpers/index.js'

describe('get_note_graph tool', () => {
  test('should be defined and have correct name', () => {
    const getNoteGraphTool = findTool('get_note_graph')
    expect(getNoteGraphTool).toBeDefined()
    expect(getNoteGraphTool.name).toBe('get_note_graph')
  })
})
