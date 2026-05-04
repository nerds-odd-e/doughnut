import { z } from 'zod'
import { createTool } from './tool-builder.js'
import {
  createErrorResponse,
  extractNoteId,
  extractTokenLimit,
  jsonResponse,
} from '../helpers.js'
import type { ToolResponse } from '../types.js'
import { NoteController } from '@generated/doughnut-backend-api/sdk.gen'

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
  noteId: number,
  tokenLimit: number
): Promise<ToolResponse> {
  const response = await NoteController.getGraph({
    path: { note: noteId },
    query: { tokenLimit },
  })

  // OpenAPI client returns { data, error, request, response } structure by default
  if (response.error) {
    throw new Error(
      `Focus context retrieval error: ${JSON.stringify(response.error)}`
    )
  }

  const graph = response.data
  return jsonResponse(graph)
}

// Tool definition with co-located logic
export const getNoteGraphTool = createTool(
  'get_note_graph',
  `Retrieves structured **focus context** for one note: the focus note plus related notes discovered through wiki links, inbound references, and sampled folder peers.

What this returns (JSON):
- **focusNote** — The requested note (depth 0) with title, notebook, folder path, truncated details when needed, and lightweight lists: **outgoingLinks** and **inboundReferences** (wiki-style link strings), plus **sampleSiblings** (a small capped sample of peers in the same folder or notebook root).
- **relatedNotes** — Notes reached within a token budget and max traversal depth. Each entry includes **depth**, **retrievalPath** (how the note was reached from the focus), **edgeType** (\`OutgoingWikiLink\`, \`InboundWikiReference\`, or \`FolderSibling\`), and truncated **details** when needed.

Use cases:
- Inspect how a note connects to the rest of the notebook before answering questions about it.
- After search, pull bounded context around a candidate note without loading the whole notebook.

You MUST obtain a valid note ID first using 'find_most_relevant_note' unless the user explicitly provides a numeric note ID.
You MUST provide a valid token limit to ensure the response fits within constraints alongside with the note ID. Your choice of the limit should consider the currently available context window. Example of a valid token limits:
1. If the note is short and the context window is large, you might set a higher token limit (e.g., 5000 tokens).
2. If the note is long or the context window is small, you might need to set a lower token limit (e.g., 500 tokens).

Navigation pattern:
1. Use 'find_most_relevant_note' to find relevant notes
2. Extract note ID from search results
3. Use this tool to load focus context around that note
4. Follow **outgoingLinks** / **relatedNotes** titles for deeper exploration if needed`,
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
  return await getNoteById(noteId, tokenLimit)
})
