import { z } from 'zod'
import type { NoteCreationDTO } from '@generated/backend/models/NoteCreationDTO.js'
import type { McpNoteAddDTO } from '@generated/backend/models/McpNoteAddDTO.js'
import type { McpAddNoteResponseDTO } from '@generated/backend/models/McpAddNoteResponseDTO.js'
import { createTool } from './tool-builder.js'
import { jsonResponse } from '../utils.js'

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
  'Add a note to a notebook, if the user specifies a notebook directly call add_note. If the user does not specify a notebook, call get_notebook_list to find a relevant notebook to add the note to, call then call add_note. Returns the title of the created note',
  AddNoteParamsSchema
).handle(async (ctx, { parentTitle, newTitle }) => {
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
})
