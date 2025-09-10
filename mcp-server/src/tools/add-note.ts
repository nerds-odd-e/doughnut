import { z } from 'zod'
import type { NoteCreationDTO } from '@generated/backend/models/NoteCreationDTO.js'
import type { McpNoteAddDTO } from '@generated/backend/models/McpNoteAddDTO.js'
import type { NoteCreationResult } from '@generated/backend/models/NoteCreationResult.js'
import { createTool } from './tool-builder.js'
import { jsonResponse } from '../helpers.js'

// Schema definition co-located with the tool
const AddNoteParamsSchema = z.object({
  parentTitle: z
    .string()
    .describe('The title of the PARENT note to add the new note to.'),
  newTitle: z.string().describe('The new title for the note.'),
})

// Tool definition with co-located logic
export const addNoteTool = createTool(
  'add_note',
  'Add a note to a notebook. Returns the title of the created note',
  AddNoteParamsSchema
).handle(async (ctx, { parentTitle, newTitle }) => {
  const noteCreationDTO: NoteCreationDTO = {
    newTitle: newTitle,
  }
  const mcpCreationDto: McpNoteAddDTO = {
    parentNote: parentTitle,
    noteCreationDTO: noteCreationDTO,
  }
  const result: NoteCreationResult =
    await ctx.api.mcpNoteCreationController.createNote1(mcpCreationDto)

  return jsonResponse({
    title: result.created.note.noteTopology.titleOrPredicate,
    message: `Added "${result.created.note.noteTopology.titleOrPredicate}" to parent "${result.parent.note.noteTopology.titleOrPredicate}"`,
  })
})
