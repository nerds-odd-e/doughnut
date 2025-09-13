import type { ToolDescriptor } from '../types.js'
import { getNoteGraphTool } from './get-graph-with-note-id.js'
import { addNoteTool } from './add-note.js'
import { findMostRelevantNoteTool } from './get-relevant-note.js'

export const tools: ToolDescriptor[] = [
  getNoteGraphTool.build(),
  addNoteTool.build(),
  findMostRelevantNoteTool.build(),
]
