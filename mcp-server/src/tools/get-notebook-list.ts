import { z } from 'zod'
import { createTool } from './tool-builder.js'
import { createErrorResponse } from '../helpers.js'

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

  return {
    content: [
      {
        type: 'text',
        text: JSON.stringify(
          notebooks.map((n) => ({
            title: n.title ?? '',
            headNoteId: n.headNoteId ?? null,
          }))
        ),
      },
    ],
  }
})
