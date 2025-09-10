import type { ToolDescriptor } from '../types.js'
import { getGraphWithNoteIdTool } from './get-graph-with-note-id.js'
import { addNoteTool } from './add-note.js'
import { getRelevantNoteTool } from './get-relevant-note.js'

export const tools: ToolDescriptor[] = [
  getGraphWithNoteIdTool.build(),
  addNoteTool.build(),
  getRelevantNoteTool.build(),
]
