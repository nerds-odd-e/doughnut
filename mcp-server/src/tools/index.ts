import type { ToolDescriptor } from '../types.js'
import { getNoteGraphTool } from './get-note-graph.js'
import { addNoteTool } from './add-note.js'
import { findMostRelevantNoteTool } from './find-most-relevant-note.js'

export const tools: ToolDescriptor[] = [
  getNoteGraphTool.build(),
  addNoteTool.build(),
  findMostRelevantNoteTool.build(),
]
