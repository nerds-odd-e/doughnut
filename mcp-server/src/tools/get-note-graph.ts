import { z } from 'zod'
import { createTool } from './tool-builder.js'
import {
  createErrorResponse,
  extractNoteId,
  extractTokenLimit,
  jsonResponse,
} from '../helpers.js'
import type { ToolResponse } from '../types.js'
import type { DoughnutApi } from '@generated/backend/DoughnutApi.js'

// Schema definition co-located with the tool
const NoteIdParamsSchema = z.object({
  noteId: z
    .number()
    .describe(
      "Numeric ID of the note to explore. Obtain this from 'find_most_relevant_note' results or user-provided ID."
    ),
  tokenLimit: z
    .number()
    .describe(
      'A valid token limit to ensure the response fits within constraints alongside with the note ID. Your choice of the limit should consider the currently available context window.'
    ),
})

// Note operations
async function getNoteById(
  api: DoughnutApi,
  noteId: number,
  tokenLimit: number
): Promise<ToolResponse> {
  const graph = await api.restNoteController.getGraph(noteId, tokenLimit)
  return jsonResponse(graph)
}

// Tool definition with co-located logic
export const getNoteGraphTool = createTool(
  'get_note_graph',
  `Retrieves the note graph - a specific note along with its surrounding context and relationships.

What This Returns:
- Focus note: The requested note with full details
- Parent notes: Hierarchical path showing where this note sits in the knowledge structure
- Child notes: Direct sub-notes under this note
- Sibling notes: Other notes at the same level
- Related notes: Semantically connected notes from other parts of the knowledge base

Use Cases:
- Understanding note context and position in knowledge hierarchy
- Finding related information after a search
- Exploring knowledge connections and discovering new insights
- Building comprehensive understanding of a topic

You MUST obtain a valid note ID first using 'find_most_relevant_note' unless the user explicitly provides a numeric note ID.
You MUST provide a valid token limit to ensure the response fits within constraints alongside with the note ID. Your choice of the limit should consider the currently available context window. Example of a valid token limits:
1. If the note is short and the context window is large, you might set a higher token limit (e.g., 5000 tokens).
2. If the note is long or the context window is small, you might need to set a lower token limit (e.g., 500 tokens).

Navigation Pattern:
1. Use 'find_most_relevant_note' to find relevant notes
2. Extract note ID from search results
3. Use this tool to explore relationships and context
4. Follow related notes for deeper exploration`,
  NoteIdParamsSchema
).handle(async (ctx, args, request) => {
  const noteId = extractNoteId(args, request)
  if (noteId === null) {
    return createErrorResponse('noteId must be provided as a number')
  }

  const tokenLimit = extractTokenLimit(args, request)

  if (tokenLimit === null || isNaN(tokenLimit)) {
    return createErrorResponse('tokenLimit must be provided as a number')
  }
  if (tokenLimit === 0) {
    return createErrorResponse('tokenLimit must be a positive number')
  }
  if (tokenLimit <= 5) {
    return createErrorResponse('tokenLimit too low to fetch any note')
  }
  // You can use the tokenLimit variable as needed in your logic here
  return await getNoteById(ctx.api, noteId, tokenLimit)
})
