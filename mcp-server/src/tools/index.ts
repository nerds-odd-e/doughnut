import type { ToolDescriptor } from '../types.js'
import type { NoteCreationDTO } from '@generated/backend/models/NoteCreationDTO.js'
import type { McpNoteAddDTO } from '@generated/backend/models/McpNoteAddDTO.js'
import type { McpAddNoteResponseDTO } from '@generated/backend/models/McpAddNoteResponseDTO.js'
import {
  emptyObjectSchema,
  updateNoteTextContentSchema,
  getGraphWithNoteIdSchema,
  addNotewithNoteTitleSchema,
  getRelevantNoteSchema,
  UpdateNoteParamsSchema,
  AddNoteParamsSchema,
  SearchNoteParamsSchema,
  SearchResultSchema,
} from '../schemas.js'
import {
  createErrorResponse,
  textResponse,
  jsonResponse,
  createToolHandler,
  extractParams,
  getNoteById,
  extractNoteId,
  formatNotebookListResponse,
} from '../utils.js'

export const tools: ToolDescriptor[] = [
  {
    name: 'update_note_text_content',
    description:
      'Update the title and/or details of a note by note ID. At least one of newTitle or newDetails must be provided. Authentication token is taken from the mcpToken argument.',
    inputSchema: updateNoteTextContentSchema,
    handle: createToolHandler(async (ctx, args, request) => {
      if (!ctx.authToken) {
        return createErrorResponse(
          'DOUGHNUT_API_AUTH_TOKEN environment variable is required.'
        )
      }

      const validation = extractParams(args, UpdateNoteParamsSchema)
      if (!validation.success) {
        return createErrorResponse(validation.error)
      }

      const { noteId, newTitle, newDetails } = validation.data

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
        detailsResult =
          await ctx.api.restTextContentController.updateNoteDetails(noteId, {
            details: newDetails,
          })
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
    }),
  },
  {
    name: 'get_notebook_list',
    description: 'Get notebook list',
    inputSchema: emptyObjectSchema,
    handle: createToolHandler(async (ctx) => {
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
    }),
  },
  {
    name: 'get_graph_with_note_id',
    description: 'Get graph with note id',
    inputSchema: getGraphWithNoteIdSchema,
    handle: createToolHandler(async (ctx, args, request) => {
      const noteId = extractNoteId(args, request)
      if (noteId === null) {
        return createErrorResponse('noteId must be provided as a number')
      }
      return await getNoteById(ctx.api, noteId)
    }),
  },
  {
    name: 'add_note',
    description:
      'Add a note to a notebook, if the user specifies a notebook directly call add_note. If the user does not specify a notebook, call get_notebook_list to find a relevant notebook to add the note to, call then call add_note. Returns the title of the created note',
    inputSchema: addNotewithNoteTitleSchema,
    handle: createToolHandler(async (ctx, args, request) => {
      const validation = extractParams(args, AddNoteParamsSchema)
      if (!validation.success) {
        return createErrorResponse(validation.error)
      }

      const { parentTitle, newTitle } = validation.data

      const noteCreationDTO: NoteCreationDTO = {
        newTitle: newTitle,
      }
      const mcpCreationDto: McpNoteAddDTO = {
        parentNote: parentTitle,
        noteCreationDTO: noteCreationDTO,
      }
      const response: McpAddNoteResponseDTO =
        await ctx.api.mcpNoteCreationController.createNote1(mcpCreationDto)

      return jsonResponse(response)
    }),
  },
  {
    name: 'get_relevant_note',
    description:
      'Given a user search request, returns the most relevant note information',
    inputSchema: getRelevantNoteSchema,
    handle: createToolHandler(async (ctx, args, request) => {
      const validation = extractParams(args, SearchNoteParamsSchema)
      if (!validation.success) {
        return createErrorResponse(validation.error)
      }

      const { query } = validation.data
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
    }),
  },
]
