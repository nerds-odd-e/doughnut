import type { ToolDescriptor } from '../types.js'
import type { NoteCreationDTO } from '@generated/backend/models/NoteCreationDTO.js'
import {
  emptyObjectSchema,
  updateNoteTextContentSchema,
  getGraphWithNoteIdSchema,
  addNotewithNoteIdSchema,
  getRelevantNoteSchema,
} from '../schemas.js'
import {
  createErrorResponse,
  textResponse,
  validateNoteUpdateParams,
} from '../utils.js'

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
          ? (viewed.notebooks as Array<{ title?: string }>)
          : null
        if (!notebooks) {
          return createErrorResponse(
            `Unexpected response from myNotebooks: ${JSON.stringify(notebooksViewed)}`
          )
        }
        const noteBookTitle = notebooks
          .map((n) => n.title ?? '')
          .filter((t) => t && t.length > 0)
          .join(', ')
        return textResponse(noteBookTitle)
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
        const graph = await api.restNoteController.getGraph(noteId)
        return textResponse(JSON.stringify(graph))
      } catch (err) {
        return createErrorResponse(err)
      }
    },
  },
  {
    name: 'add_note',
    description: 'Add a note to the notebook',
    inputSchema: addNotewithNoteIdSchema,
    handle: async (ctx, args, request) => {
      const api = ctx.api
      try {
        const noteId = Number(
          (args as { noteId?: number }).noteId ??
            (request as { params?: { noteId?: number } }).params?.noteId
        )

        const newTitle = String(
          (args as { newTitle?: string }).newTitle ??
            (request as { params?: { newTitle?: string } }).params?.newTitle
        )

        const noteCreationDto: NoteCreationDTO = {
          newTitle: newTitle,
        }
        await api.mcpNoteCreationController.createNote1(noteId, noteCreationDto)

        return textResponse('All Good')
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
      let query = ''
      if (typeof args === 'object' && args !== null) {
        if ('args' in args && typeof args.args === 'string') {
          query = args.args
        } else if ('query' in args) {
          query = (args as { query: string }).query
        }
      } else if (typeof args === 'string') {
        query = args
      }
      try {
        const searchTerm = {
          searchKey: query,
          allMyNotebooksAndSubscriptions: true,
        }
        const results =
          await api.restSearchController.searchForLinkTarget(searchTerm)
        if (
          Array.isArray(results) &&
          results.length > 0 &&
          typeof results[0].noteTopology.id === 'number'
        ) {
          const noteId = results[0].noteTopology.id.toString()
          const graph = await api.restNoteController.getGraph(noteId)
          return textResponse(JSON.stringify(graph))
        }
        return textResponse(
          `${JSON.stringify(args)}·${query}·No relevant note found.`
        )
      } catch (err) {
        return createErrorResponse(err)
      }
    },
  },
]
