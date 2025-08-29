import type { ServerApi, ToolDescriptor } from '../types.js'
import type { NoteCreationDTO } from '@generated/backend/models/NoteCreationDTO.js'
import {
  emptyObjectSchema,
  updateNoteTextContentSchema,
  getGraphWithNoteIdSchema,
  addNotewithNoteTitleSchema,
  getRelevantNoteSchema,
} from '../schemas.js'
import {
  createErrorResponse,
  textResponse,
  validateNoteUpdateParams,
} from '../utils.js'
import type { McpNoteAddDTO } from '@generated/backend/models/McpNoteAddDTO.js'
import type { McpAddNoteResponse } from '@generated/backend/models/McpAddNoteResponse.js'
import { z } from 'zod'

export const tools: ToolDescriptor[] = [
  {
    name: 'get_instruction',
    description: 'Get instruction',
    inputSchema: emptyObjectSchema,
    handle: async () =>
      textResponse('Doughnut is a Personal Knowledge Management tool'),
  },

  {
    name: 'update_note_text_content',
    description:
      'Update the title and/or details of a note by note ID. At least one of newTitle or newDetails must be provided. Authentication token is taken from the mcpToken argument.',
    inputSchema: updateNoteTextContentSchema,
    handle: async (ctx, args) => {
      const { noteId, newTitle, newDetails } = args as {
        noteId?: number
        newTitle?: string | null
        newDetails?: string | null
      }
      if (!ctx.authToken) {
        return createErrorResponse(
          'DOUGHNUT_API_AUTH_TOKEN environment variable is required.'
        )
      }
      const validation = validateNoteUpdateParams(
        typeof noteId === 'number' ? noteId : undefined,
        newTitle,
        newDetails
      )
      if (!validation.isValid) return createErrorResponse(validation.error!)

      const api = ctx.api
      let titleResult: unknown = null
      let detailsResult: unknown = null
      try {
        if (typeof newTitle === 'string') {
          titleResult = await api.restTextContentController.updateNoteTitle(
            noteId!,
            { newTitle }
          )
        }
        if (typeof newDetails === 'string') {
          detailsResult = await api.restTextContentController.updateNoteDetails(
            noteId!,
            { details: newDetails }
          )
        }
      } catch (err) {
        return createErrorResponse(err, 'Failed to update note:')
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
    },
  },
  {
    name: 'get_user_info',
    description: 'Get user info',
    inputSchema: emptyObjectSchema,
    handle: async (ctx) => {
      const api = ctx.api
      try {
        const userInfo = await api.restUserController.getUserProfile()
        return textResponse(JSON.stringify(userInfo))
      } catch (err) {
        return createErrorResponse(err)
      }
    },
  },
  {
    name: 'get_notebook_list',
    description: 'Get notebook list',
    inputSchema: emptyObjectSchema,
    handle: async (ctx) => {
      const api = ctx.api
      try {
        const notebooksViewed = await api.restNotebookController.myNotebooks()
        const viewed = notebooksViewed as { notebooks?: unknown }
        const notebooks = Array.isArray(viewed.notebooks)
          ? (viewed.notebooks as Array<{ title?: string; headNoteId?: number }>)
          : null
        if (!notebooks) {
          return createErrorResponse(
            `Unexpected response from myNotebooks: ${JSON.stringify(notebooksViewed)}`
          )
        }
        // Return notebooks with headNoteId included
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
      } catch (err) {
        return createErrorResponse(err)
      }
    },
  },
  {
    name: 'get_graph_with_note_id',
    description: 'Get graph with note id',
    inputSchema: getGraphWithNoteIdSchema,
    handle: async (ctx, args, request) => {
      const api = ctx.api
      try {
        // Support both args.noteId and request.params.noteId (for compatibility)
        const noteId = Number(
          (args as { noteId?: number }).noteId ??
            (request as { params?: { noteId?: number } }).params?.noteId
        )
        return await GetNoteByNoteId(api, noteId)
      } catch (err) {
        return createErrorResponse(err)
      }
    },
  },
  {
    name: 'add_note',
    description:
      'Add a note to a notebook, if the user specifies a notebook directly call add_note. If the user does not specify a notebook, call get_notebook_list to find a relevant notebook to add the note to, call then call add_note. Returns the title of the created note',
    inputSchema: addNotewithNoteTitleSchema,
    handle: async (ctx, args) => {
      const api = ctx.api
      try {
        const parentTitle = String(
          (args as { parentTitle?: string }).parentTitle
        )

        const newTitle = String((args as { newTitle?: string }).newTitle)

        const noteCreationDTO: NoteCreationDTO = {
          newTitle: newTitle,
        }
        const mcpCreationDto: McpNoteAddDTO = {
          parentNote: parentTitle,
          noteCreationDTO: noteCreationDTO,
        }
        const response: McpAddNoteResponse =
          await api.mcpNoteCreationController.createNote1(mcpCreationDto)

        return textResponse(JSON.stringify(response))
      } catch (err) {
        return createErrorResponse(err)
      }
    },
  },
  {
    name: 'get_relevant_note',
    description:
      'Given a user search request, returns the most relevant note information',
    inputSchema: getRelevantNoteSchema,
    handle: async (ctx, args) => {
      const api = ctx.api

      try {
        const query = args.query as string
        const searchTerm = {
          searchKey: query,
          allMyNotebooksAndSubscriptions: true,
        }
        const results =
          await api.restSearchController.searchForLinkTarget(searchTerm)

        // Use zod to validate each result item
        const resultSchema = z.object({
          noteTopology: z.object({
            id: z.number(),
          }),
        })

        if (Array.isArray(results) && results.length > 0) {
          const parseResult = resultSchema.safeParse(results[0])
          if (parseResult.success) {
            const noteId = parseResult.data.noteTopology.id
            return await GetNoteByNoteId(api, noteId)
          }
        }
        return textResponse(`No relevant note found.`)
      } catch (err) {
        if (err instanceof Error && err.message === 'Invalid Input.') {
          return textResponse('Invalid Input.')
        }
        return createErrorResponse(err)
      }
    },
  },
]

async function GetNoteByNoteId(api: ServerApi, noteId: number) {
  const graph = await api.restNoteController.getGraph(noteId)
  return textResponse(JSON.stringify(graph))
}
