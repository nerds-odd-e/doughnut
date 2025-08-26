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

export const addNotewithNoteIdSchema = {
  type: 'object',
  properties: {
    noteId: {
      type: 'integer',
      description: 'The ID of the note to fetch graph for.',
    },
    newTitle: {
      type: ['string', 'null'],
      description: 'The new title for the note.',
    },
  },
} as const

export const getRelevantNoteIdSchema = {
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
