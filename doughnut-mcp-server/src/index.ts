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
    ],
  }
})

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
