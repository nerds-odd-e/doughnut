export const emptyObjectSchema = {
  type: 'object',
} as const

export const updateNoteTextContentSchema = {
  type: 'object',
  properties: {
    noteId: {
      type: 'integer',
      description: 'The ID of the note to update.',
    },
    newTitle: {
      type: 'string',
      description: 'The new title for the note.',
      nullable: true,
    },
    newDetails: {
      type: 'string',
      description: 'The new details for the note.',
      nullable: true,
    },
  },
  required: ['noteId'],
} as const
