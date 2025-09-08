import { z } from 'zod'
import { createTool } from './tool-builder.js'
import { textResponse, getNoteById } from '../utils.js'

// Schema definitions co-located with the tool
const SearchNoteParamsSchema = z.object({
  query: z
    .string()
    .describe(
      'The user search request. The most relevant note id (if any) will be returned.'
    ),
})

const SearchResultSchema = z.object({
  noteTopology: z.object({
    id: z.number(),
  }),
})

// Tool definition with co-located logic
export const getRelevantNoteTool = createTool(
  'get_relevant_note',
  'Given a user search request, returns the most relevant note information',
  SearchNoteParamsSchema
).handle(async (ctx, { query }) => {
  const searchTerm = {
    searchKey: query,
    allMyNotebooksAndSubscriptions: true,
  }

  try {
    const results =
      await ctx.api.restSearchController.searchForLinkTarget(searchTerm)

    if (Array.isArray(results) && results.length > 0) {
      const parseResult = SearchResultSchema.safeParse(results[0])
      if (parseResult.success) {
        const noteId = parseResult.data.noteTopology.id
        return await getNoteById(ctx.api, noteId)
      }
    }
    return textResponse('No relevant note found.')
  } catch (err) {
    if (err instanceof Error && err.message === 'Invalid Input.') {
      return textResponse('Invalid Input.')
    }
    throw err // Let createToolHandler handle other errors
  }
})
