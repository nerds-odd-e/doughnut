import { Client } from '@modelcontextprotocol/sdk/client/index.js'
import { StdioClientTransport } from '@modelcontextprotocol/sdk/client/stdio.js'

let client: Client | null = null

export const getMcpClient = () => {
  if (!client) {
    client = new Client(
      {
        name: 'doughnut-mcp-client',
        version: '1.0.0',
      },
      {
        capabilities: {},
      }
    )
  }
  return client
}

/**
 * Connects the MCP client to the doughnut-mcp-server using stdio.
 * Spawns the server as a child process and connects via StdioClientTransport.
 */
export const connectMcpClient = async () => {
  const client = getMcpClient()
  // Only connect once
  if ((client as any)?._connected) {
    return true
  }

  // Use absolute path to the built server entry point
  const serverPath =
    '../../backend/src/main/resources/static/js/mcp-server.bundle.js'

  // Optionally, check if the file exists for better error reporting
  const fs = require('fs')
  if (!fs.existsSync(serverPath)) {
    throw new Error(`MCP server entry not found at ${serverPath}`)
  }

  // Let the SDK spawn the process: pass command as array ['node', serverPath]
  const transport = new StdioClientTransport({
    command: process.execPath,
    args: [serverPath],
  })
  await client.connect(transport)
  ;(client as any)._connected = true
  return true
}
