import { z } from 'zod'
import type {
  NoteCreationDTO,
  McpNoteAddDTO,
  NoteCreationResult,
} from '@generated/backend'
import { createTool } from './tool-builder.js'
import { jsonResponse } from '../helpers.js'

// Schema definition co-located with the tool
const AddNoteParamsSchema = z.object({
  parentTitle: z
    .string()
    .describe(
      'Exact title of the parent note where the new note will be created. Must match an existing note title exactly.'
    ),
  newTitle: z
    .string()
    .describe(
      "Title for the new note. Should be specific, clear, and fit well within the parent's context."
    ),
})

// Tool definition with co-located logic
export const addNoteTool = createTool(
  'add_note',
  `Creates a new note under an existing parent note in the knowledge hierarchy.

Prerequisites:
- You MUST identify the correct parent note first using 'find_most_relevant_note'
- Parent note must exist and be accessible to the user
- Use exact parent title as returned from search results

Note Creation Process:
1. Search for intended parent note to verify it exists
2. Use the exact title from search results as parentTitle parameter
3. Choose a clear, specific title that fits the knowledge context
4. Note will be created with the title only (use separate tools to add content)

Title Guidelines:
- Be specific and descriptive
- Avoid duplicating parent context in the title
- Use consistent naming conventions within the knowledge area
- Consider how this fits into the broader knowledge structure

Response Format:
- Returns the created note's title and confirmation message
- The created note will be empty initially - use content management tools to add details`,
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
