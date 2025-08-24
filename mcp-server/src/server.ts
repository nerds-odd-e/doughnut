import { Server } from '@modelcontextprotocol/sdk/server/index.js'
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from '@modelcontextprotocol/sdk/types.js'
import type { ServerContext, ToolDescriptor } from './types.js'

export function createServer(tools: ToolDescriptor[], context: ServerContext) {
  const server = new Server(
    { name: 'doughnut-mcp-server', version: '0.1.0' },
    { capabilities: { resources: {}, tools: {}, prompts: {} } }
  )

  server.setRequestHandler(ListToolsRequestSchema, async () => {
    return {
      tools: tools.map((t) => ({
        name: t.name,
        description: t.description,
        inputSchema: t.inputSchema,
      })),
    }
  })

  server.setRequestHandler(CallToolRequestSchema, async (request) => {
    const tool = tools.find((t) => t.name === request.params.name)
    if (!tool) throw new Error('Unknown tool')

    // MCP SDK can pass arguments either in params.arguments or directly
    const rp = request.params as unknown as {
      arguments?: Record<string, unknown>
    }
    const args: Record<string, unknown> =
      rp.arguments && typeof rp.arguments === 'object' ? rp.arguments : (request.params as unknown as Record<string, unknown>)
    return await tool.handle(context, args, request)
  })

  return server
}
