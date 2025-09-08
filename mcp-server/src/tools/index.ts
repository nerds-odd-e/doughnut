import type { ToolDescriptor } from '../types.js'
import { updateNoteTextContentTool } from './update-note-text-content.js'
import { getNotebookListTool } from './get-notebook-list.js'
import { getGraphWithNoteIdTool } from './get-graph-with-note-id.js'
import { addNoteTool } from './add-note.js'
import { getRelevantNoteTool } from './get-relevant-note.js'

export const tools: ToolDescriptor[] = [
  updateNoteTextContentTool.build(),
  getNotebookListTool.build(),
  getGraphWithNoteIdTool.build(),
  addNoteTool.build(),
  getRelevantNoteTool.build(),
]
