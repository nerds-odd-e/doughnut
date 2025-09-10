import { z } from 'zod'
import { createTool } from './tool-builder.js'
import { createErrorResponse, textResponse } from '../helpers.js'

// Schema definition co-located with the tool
const UpdateNoteParamsSchema = z.object({
  noteId: z.number().describe('The ID of the note to update.'),
  newTitle: z
    .string()
    .nullable()
    .optional()
    .describe('The new title for the note.'),
  newDetails: z
    .string()
    .nullable()
    .optional()
    .describe('The new details for the note.'),
})

// Tool definition with co-located logic
export const updateNoteTextContentTool = createTool(
  'update_note_text_content',
  'Update the title and/or details of a note by note ID. At least one of newTitle or newDetails must be provided. Authentication token is taken from the mcpToken argument.',
  UpdateNoteParamsSchema
).handle(async (ctx, { noteId, newTitle, newDetails }) => {
  if (!ctx.authToken) {
    return createErrorResponse(
      'DOUGHNUT_API_AUTH_TOKEN environment variable is required.'
    )
  }

  // Validate at least one field is provided
  if (typeof newTitle !== 'string' && typeof newDetails !== 'string') {
    return createErrorResponse(
      'At least one of newTitle or newDetails must be provided.'
    )
  }

  let titleResult: unknown = null
  let detailsResult: unknown = null

  if (typeof newTitle === 'string') {
    titleResult = await ctx.api.restTextContentController.updateNoteTitle(
      noteId,
      { newTitle }
    )
  }
  if (typeof newDetails === 'string') {
    detailsResult = await ctx.api.restTextContentController.updateNoteDetails(
      noteId,
      {
        details: newDetails,
      }
    )
  }

  let msg = 'Note updated successfully.'
  const titleRes = titleResult as {
    note?: { noteTopology?: { titleOrPredicate?: string } }
  } | null
  if (titleRes?.note?.noteTopology?.titleOrPredicate) {
    msg += ` Title: ${titleRes.note.noteTopology.titleOrPredicate}.`
  }
  const detailsRes = detailsResult as { note?: { details?: string } } | null
  if (detailsRes?.note?.details) {
    msg += ` Details: ${detailsRes.note.details}.`
  }
  return textResponse(msg)
})
