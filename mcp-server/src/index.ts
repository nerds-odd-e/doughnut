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
import type { NoteUpdateResult } from './types.js'

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
      // Always use authToken from environment variable
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

      let titleResult: NoteUpdateResult | null = null
      let detailsResult: NoteUpdateResult | null = null
      // Update title if provided
      if (typeof newTitle === 'string') {
        const titleResponse = await fetch(
          `${DOUGHNUT_API_BASE_URL}/api/text_content/${noteId}/title`,
          {
            method: 'PATCH',
            headers: {
              'Content-Type': 'application/json',
              Authorization: `Bearer ${authToken}`,
            },
            body: JSON.stringify({ newTitle }),
          }
        )
        if (!titleResponse.ok) {
          return {
            content: [
              {
                type: 'text',
                text: `Failed to update note title: ${titleResponse.status} ${await titleResponse.text()}`,
              },
            ],
          }
        }
        titleResult = await titleResponse.json()
      }
      // Update details if provided
      if (typeof newDetails === 'string') {
        const detailsResponse = await fetch(
          `${DOUGHNUT_API_BASE_URL}/api/text_content/${noteId}/details`,
          {
            method: 'PATCH',
            headers: {
              'Content-Type': 'application/json',
              Authorization: `Bearer ${authToken}`,
            },
            body: JSON.stringify({ details: newDetails }),
          }
        )
        if (!detailsResponse.ok) {
          return {
            content: [
              {
                type: 'text',
                text: `Failed to update note details: ${detailsResponse.status} ${await detailsResponse.text()}`,
              },
            ],
          }
        }
        detailsResult = await detailsResponse.json()
      }
      // Compose result message
      let msg = 'Note updated successfully.'
      if (
        titleResult &&
        titleResult.note &&
        titleResult.note.topicConstructor
      ) {
        msg += ` Title: ${titleResult.note.topicConstructor}.`
      }
      if (detailsResult && detailsResult.note && detailsResult.note.details) {
        msg += ` Details: ${detailsResult.note.details}.`
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
        const response = await fetch(
          `${DOUGHNUT_API_BASE_URL}/api/notebooks/get-notebook-list`,
          {
            method: 'GET',
            headers: {
              mcpToken: authToken,
            },
          }
        )
        const data = await response.json()
        if (!Array.isArray(data)) {
          return {
            content: [
              {
                type: 'text',
                text: `ERROR: Unexpected response from get-notebook-list: ${JSON.stringify(data)}`,
              },
            ],
          }
        }
        const noteBookTitle = data.map((n: any) => n.title).join(', ')
        return {
          content: [
            {
              type: 'text',
              text: noteBookTitle,
            },
          ],
        }
      } catch (err) {
        const errorMsg = (err as any).message || String(err)
        return {
          content: [
            {
              type: 'text',
              text: `ERROR: ${errorMsg}`,
            },
          ],
        }
      }
    }
    case 'get_user_info': {
      const apiUrl = `${DOUGHNUT_API_BASE_URL}/api/user/info`
      try {
        const response = await fetch(apiUrl, {
          method: 'GET',
          headers: {
            mcpToken: authToken,
          },
        })
        const text = await response.text()
        return {
          content: [
            {
              type: 'text',
              text,
            },
          ],
        }
      } catch (err) {
        const errorMsg = (err as any).message || String(err)
        return {
          content: [
            {
              type: 'text',
              text: `ERROR: ${errorMsg}`,
            },
          ],
        }
      }
    }

    case 'get_graph_with_note_id': {
      const apiUrl = `${DOUGHNUT_API_BASE_URL}/api/notes/${request.params.noteId}/graph`

      try {
        const response = await fetch(apiUrl, {
          method: 'GET',
        })
        const text = await response.text()
        return {
          content: [
            {
              type: 'text',
              text,
            },
          ],
        }
      } catch (err) {
        const errorMsg = (err as any).message || String(err)
        return {
          content: [
            {
              type: 'text',
              text: `ERROR: ${errorMsg}`,
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
