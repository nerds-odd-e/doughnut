import { describe, test, expect } from 'vitest'
import { findTool } from '../helpers/index.js'

describe('update_note_text_content tool', () => {
  test('should be defined and have correct name', () => {
    const updateNoteTextContentTool = findTool('update_note_text_content')
    expect(updateNoteTextContentTool).toBeDefined()
    expect(updateNoteTextContentTool.name).toBe('update_note_text_content')
  })
})
