import { z } from 'zod'
import { createTool } from './tool-builder.js'
import { textResponse, jsonResponse } from '../helpers.js'
import { SearchController } from '@generated/donut-backend-api/sdk.gen'

const ListNoteCandidatesParamsSchema = z.object({
  query: z
    .string()
    .describe(
      'Search text matched against note titles and aliases, e.g. a note title or a keyword it contains.'
    ),
})

export const listNoteCandidatesTool = createTool(
  'list_note_candidates',
  `Lists every note whose title or alias matches the query text, as candidates to compare before choosing one.

Search Behavior:
- Matches the query text against note titles and aliases
- Covers all user's notebooks and subscribed notebooks
- Returns note candidates only, in the search's ranking order

Response Format:
- JSON array; each candidate has noteId, title, notebookName, folderName (omitted at notebook root), and distance
- Candidates with equal titles are told apart by notebookName and folderName
- If no note matches, returns "No matching notes."
- Use a candidate's noteId with 'read_note' to read it in full for citing, or with 'get_note_graph' for its context and connections

For best results, search with a likely note title or a distinctive word from it.`,
  ListNoteCandidatesParamsSchema
).handle(async (_ctx, { query }) => {
  const response = await SearchController.searchForRelationshipTarget({
    body: { searchKey: query, allMyNotebooksAndSubscriptions: true },
  })

  if (response.error) {
    return textResponse(`Search error: ${JSON.stringify(response.error)}`)
  }

  const candidates = (response.data ?? []).flatMap(({ noteSearchResult }) =>
    noteSearchResult
      ? [
          {
            noteId: noteSearchResult.noteTopology.id,
            title: noteSearchResult.noteTopology.title,
            notebookName: noteSearchResult.notebookName,
            folderName: noteSearchResult.folderName,
            distance: noteSearchResult.distance,
          },
        ]
      : []
  )

  if (candidates.length === 0) {
    return textResponse('No matching notes.')
  }
  return jsonResponse(candidates)
})
