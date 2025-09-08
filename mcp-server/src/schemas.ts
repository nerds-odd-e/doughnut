export const emptyObjectSchema = {
  type: 'object',
  properties: {},
  additionalProperties: false,
} as const

export const updateNoteTextContentSchema = {
  type: 'object',
  properties: {
    noteId: {
      type: 'integer',
      description: 'The ID of the note to update.',
    },
    newTitle: {
      type: ['string', 'null'],
      description: 'The new title for the note.',
    },
    newDetails: {
      type: ['string', 'null'],
      description: 'The new details for the note.',
    },
  },
  additionalProperties: false,
  required: ['noteId'],
} as const

export const getGraphWithNoteIdSchema = {
  type: 'object',
  properties: {
    noteId: {
      type: 'integer',
      description: 'The ID of the note to fetch graph for.',
    },
  },
  required: ['noteId'],
  additionalProperties: false,
} as const

export const addNotewithNoteTitleSchema = {
  type: 'object',
  properties: {
    parentTitle: {
      type: 'string',
      description: 'The title of the PARENT note to add the new note to.',
    },
    newTitle: {
      type: ['string'],
      description: 'The new title for the note.',
    },
  },
} as const

export const getRelevantNoteSchema = {
  type: 'object',
  properties: {
    query: {
      type: 'string',
      description:
        'The user search request. The most relevant note id (if any) will be returned.',
    },
  },
  required: ['query'],
  additionalProperties: false,
} as const

// Zod schemas for runtime validation and type safety
import { z } from 'zod'

export const UpdateNoteParamsSchema = z.object({
  noteId: z.number(),
  newTitle: z.string().nullable().optional(),
  newDetails: z.string().nullable().optional(),
})

export const NoteIdParamsSchema = z.object({
  noteId: z.number(),
})

export const AddNoteParamsSchema = z.object({
  parentTitle: z.string(),
  newTitle: z.string(),
})

export const SearchNoteParamsSchema = z.object({
  query: z.string(),
})

export const SearchResultSchema = z.object({
  noteTopology: z.object({
    id: z.number(),
  }),
})

export type UpdateNoteParams = z.infer<typeof UpdateNoteParamsSchema>
export type NoteIdParams = z.infer<typeof NoteIdParamsSchema>
export type AddNoteParams = z.infer<typeof AddNoteParamsSchema>
export type SearchNoteParams = z.infer<typeof SearchNoteParamsSchema>
