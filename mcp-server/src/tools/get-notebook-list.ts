import { z } from 'zod'
import { createTool } from './tool-builder.js'
import { createErrorResponse, formatNotebookListResponse } from '../utils.js'

// Schema definition co-located with the tool
const EmptyObjectSchema = z.object({})

// Tool definition with co-located logic
export const getNotebookListTool = createTool(
  'get_notebook_list',
  'Get notebook list',
  EmptyObjectSchema
).handle(async (ctx) => {
  const notebooksViewed = await ctx.api.restNotebookController.myNotebooks()
  const viewed = notebooksViewed as { notebooks?: unknown }
  const notebooks = Array.isArray(viewed.notebooks)
    ? (viewed.notebooks as Array<{ title?: string; headNoteId?: number }>)
    : null

  if (!notebooks) {
    return createErrorResponse(
      `Unexpected response from myNotebooks: ${JSON.stringify(notebooksViewed)}`
    )
  }

  return formatNotebookListResponse(notebooks)
})
