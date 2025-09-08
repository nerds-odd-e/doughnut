// Zod schemas for runtime validation and type safety
import { z } from 'zod'

// Core Zod schemas
export const EmptyObjectZodSchema = z.object({})

export const UpdateNoteParamsSchema = z.object({
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

export const NoteIdParamsSchema = z.object({
  noteId: z.number().describe('The ID of the note to fetch graph for.'),
})

export const AddNoteParamsSchema = z.object({
  parentTitle: z
    .string()
    .describe('The title of the PARENT note to add the new note to.'),
  newTitle: z.string().describe('The new title for the note.'),
})

export const SearchNoteParamsSchema = z.object({
  query: z
    .string()
    .describe(
      'The user search request. The most relevant note id (if any) will be returned.'
    ),
})

export const SearchResultSchema = z.object({
  noteTopology: z.object({
    id: z.number(),
  }),
})

// Generate JSON Schema objects from Zod schemas for MCP protocol
export const emptyObjectSchema = z.toJSONSchema(EmptyObjectZodSchema)
export const updateNoteTextContentSchema = z.toJSONSchema(
  UpdateNoteParamsSchema
)
export const getGraphWithNoteIdSchema = z.toJSONSchema(NoteIdParamsSchema)
export const addNotewithNoteTitleSchema = z.toJSONSchema(AddNoteParamsSchema)
export const getRelevantNoteSchema = z.toJSONSchema(SearchNoteParamsSchema)
