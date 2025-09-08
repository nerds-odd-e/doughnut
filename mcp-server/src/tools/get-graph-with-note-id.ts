import { z } from 'zod'
import { createTool } from './tool-builder.js'
import { createErrorResponse, getNoteById, extractNoteId } from '../utils.js'

// Schema definition co-located with the tool
const NoteIdParamsSchema = z.object({
  noteId: z.number().describe('The ID of the note to fetch graph for.'),
})

// Tool definition with co-located logic
export const getGraphWithNoteIdTool = createTool(
  'get_graph_with_note_id',
  'Get graph with note id',
  NoteIdParamsSchema
).handle(async (ctx, args, request) => {
  const noteId = extractNoteId(args, request)
  if (noteId === null) {
    return createErrorResponse('noteId must be provided as a number')
  }
  return await getNoteById(ctx.api, noteId)
})
