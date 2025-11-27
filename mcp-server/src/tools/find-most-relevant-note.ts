import { z } from 'zod'
import { createTool } from './tool-builder.js'
import { textResponse, jsonResponse } from '../helpers.js'
import type { NoteSearchResult } from '@generated/backend'
import { SearchController } from '@generated/backend/sdk.gen'

// Schema definitions co-located with the tool
const SearchNoteParamsSchema = z.object({
  query: z
    .string()
    .describe(
      "Search query - can be keywords, questions, or concepts. AI will find semantically related notes even if exact words don't match."
    ),
})

// Tool definition with co-located logic
export const findMostRelevantNoteTool = createTool(
  'find_most_relevant_note',
  `Searches across all accessible notes and returns the single most relevant note based on semantic similarity.

Search Behavior:
- Searches through note titles and content using AI-powered semantic matching
- Covers all user's notebooks and subscribed notebooks
- Returns only the single most relevant result
- Uses embeddings to find conceptually related content, not just exact text matches

When to Use:
- Finding existing information on a topic
- Locating notes before creating duplicates
- Discovering related knowledge that already exists

Response Format:
- Returns complete note information including title, content, and basic metadata
- If no relevant notes found, returns "No relevant note found."
- Use the returned note ID with 'get_note_graph' for context and connections

For broader search results, consider breaking down complex queries into specific concepts.`,
  SearchNoteParamsSchema
).handle(async (ctx, { query }) => {
  const searchTerm = {
    searchKey: query,
    allMyNotebooksAndSubscriptions: true,
  }

  try {
    const response = await SearchController.searchForLinkTarget({
      body: searchTerm,
    })

    // OpenAPI client returns { data, error, request, response } structure by default
    if (response.error) {
      return textResponse(`Search error: ${JSON.stringify(response.error)}`)
    }

    const results = response.data as NoteSearchResult[]

    if (Array.isArray(results) && results.length > 0) {
      const firstResult = results[0] as NoteSearchResult
      return jsonResponse(firstResult)
    }
    return textResponse('No relevant note found.')
  } catch (err) {
    if (err instanceof Error && err.message === 'Invalid Input.') {
      return textResponse('Invalid Input.')
    }
    throw err // Let createToolHandler handle other errors
  }
})
