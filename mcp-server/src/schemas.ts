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
