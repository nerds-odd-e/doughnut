/**
 * This is a MCP server that implements a connection to Doughnut system.
 * It demonstrates core MCP concepts like resources and tools by allowing:
 * - Get instrucion
 */

import { Server } from '@modelcontextprotocol/sdk/server/index.js'
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js'
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from '@modelcontextprotocol/sdk/types.js'
import { DoughnutApi } from '../../generated/backend/DoughnutApi.js'
import type { NoteUpdateTitleDTO } from '../../generated/backend/models/NoteUpdateTitleDTO.js'
import type { NoteUpdateDetailsDTO } from '../../generated/backend/models/NoteUpdateDetailsDTO.js'

/**
 * Create an MCP server to connect to Doughnut server
 */
const server = new Server(
  {
    name: 'doughnut-mcp-server',
    version: '0.1.0',
  },
  {
    capabilities: {
      resources: {},
      tools: {},
      prompts: {},
    },
  }
)

/**
 * Handler that lists available tools.
 * Exposes a single "get_instruction" tool that lets clients get instruction.
 */
server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
    tools: [
      {
        name: 'get_instruction',
        description: 'Get instruction',
        inputSchema: {
          type: 'object',
        },
      },
      {
        name: 'get_sampleapi',
        description: 'Get sample api',
        inputSchema: {
          type: 'object',
        },
      },
      {
        name: 'update_note_text_content',
        description:
          'Update the title and/or details of a note by note ID. At least one of newTitle or newDetails must be provided. Authentication token is taken from the mcpToken argument.',
        inputSchema: {
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
        },
      },
      {
        name: 'get_user_info',
        description: 'Get user info',
        inputSchema: {
          type: 'object',
        },
      },
      {
        name: 'get_notebook_list',
        description: 'Get notebook list',
        inputSchema: {
          type: 'object',
        },
      },
      {
        name: 'get_graph_with_note_id',
        description: 'Get graph with note id',
        inputSchema: {
          type: 'object',
        },
      },
    ],
  }
})

/**
 * Get Doughnut API base URL from environment or use default.
 */
const DOUGHNUT_API_BASE_URL =
  process.env.DOUGHNUT_API_BASE_URL || 'http://localhost:9081'
const authToken = process.argv[2]
const api = new DoughnutApi({ BASE: DOUGHNUT_API_BASE_URL, TOKEN: authToken })

/**
 * Handler for the create_note tool.
 * Creates a new note with the provided title and content, and returns success message.
 */
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  switch (request.params.name) {
    case 'get_instruction': {
      return {
        content: [
          {
            type: 'text',
            text: 'Doughnut is a Personal Knowledge Management tool',
          },
        ],
      }
    }
    case 'get_sampleapi': {
      return {
        content: [
          {
            type: 'text',
            text: 'Sample API',
          },
        ],
      }
    }
    case 'update_note_text_content': {
      let noteId: number | undefined
      let newTitle: string | null | undefined
      let newDetails: string | null | undefined
      if (
        request.params.arguments &&
        typeof request.params.arguments === 'object'
      ) {
        ;({ noteId, newTitle, newDetails } = request.params.arguments as {
          noteId: number
          newTitle?: string | null
          newDetails?: string | null
        })
      } else {
        ;({ noteId, newTitle, newDetails } = request.params as unknown as {
          noteId: number
          newTitle?: string | null
          newDetails?: string | null
        })
      }
      if (!authToken) {
        return {
          content: [
            {
              type: 'text',
              text: 'DOUGHNUT_API_AUTH_TOKEN environment variable is required.',
            },
          ],
        }
      }
      if (typeof newTitle !== 'string' && typeof newDetails !== 'string') {
        return {
          content: [
            {
              type: 'text',
              text: 'At least one of newTitle or newDetails must be provided.',
            },
          ],
        }
      }
      let titleResult: any = null
      let detailsResult: any = null
      try {
        if (typeof newTitle === 'string') {
          titleResult = await api.restTextContentController.updateNoteTitle(
            noteId!,
            { newTitle } as NoteUpdateTitleDTO
          )
        }
        if (typeof newDetails === 'string') {
          detailsResult = await api.restTextContentController.updateNoteDetails(
            noteId!,
            { details: newDetails } as NoteUpdateDetailsDTO
          )
        }
      } catch (err: any) {
        return {
          content: [
            {
              type: 'text',
              text: `Failed to update note: ${err?.message || err}`,
            },
          ],
        }
      }
      let msg = 'Note updated successfully.'
      if (titleResult && titleResult.topicConstructor) {
        msg += ` Title: ${titleResult.topicConstructor}.`
      }
      if (detailsResult && detailsResult.details) {
        msg += ` Details: ${detailsResult.details}.`
      }
      return {
        content: [
          {
            type: 'text',
            text: msg,
          },
        ],
      }
    }
    case 'get_notebook_list': {
      try {
        const notebooksViewed = await api.restNotebookController.myNotebooks()
        if (!(notebooksViewed && Array.isArray(notebooksViewed.notebooks))) {
          return {
            content: [
              {
                type: 'text',
                text: `ERROR: Unexpected response from myNotebooks: ${JSON.stringify(notebooksViewed)}`,
              },
            ],
          }
        }
        const noteBookTitle = notebooksViewed.notebooks
          .map((n: any) => n.title)
          .join(', ')
        return {
          content: [
            {
              type: 'text',
              text: noteBookTitle,
            },
          ],
        }
      } catch (err: any) {
        return {
          content: [
            {
              type: 'text',
              text: `ERROR: ${err?.message || err}`,
            },
          ],
        }
      }
    }
    case 'get_user_info': {
      try {
        const userInfo =
          await api.restUserController.getUserInfoByMcpToken(authToken)
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify(userInfo),
            },
          ],
        }
      } catch (err: any) {
        return {
          content: [
            {
              type: 'text',
              text: `ERROR: ${err?.message || err}`,
            },
          ],
        }
      }
    }
    case 'get_graph_with_note_id': {
      try {
        const noteId = Number(request.params.noteId)
        const graph = await api.restNoteController.getGraph(noteId)
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify(graph),
            },
          ],
        }
      } catch (err: any) {
        return {
          content: [
            {
              type: 'text',
              text: `ERROR: ${err?.message || err}`,
            },
          ],
        }
      }
    }

    default:
      throw new Error('Unknown tool')
  }
})

/**
 * Start the server using stdio transport.
 * This allows the server to communicate via standard input/output streams.
 */
async function main() {
  const transport = new StdioServerTransport()
  await server.connect(transport)
}

main().catch((error) => {
  console.error('Server error:', error)
  process.exit(1)
})
