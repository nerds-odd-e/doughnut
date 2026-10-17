import type { ToolDescriptor } from '../types.js'
import { getNoteGraphTool } from './get-note-graph.js'
import { listNoteCandidatesTool } from './list-note-candidates.js'

export const tools: ToolDescriptor[] = [
  getNoteGraphTool.build(),
  listNoteCandidatesTool.build(),
]
