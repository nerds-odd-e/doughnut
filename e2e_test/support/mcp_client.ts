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
export const connectMcpClient = async (backendBaseUrl: string) => {
  const client = getMcpClient()
  // Only connect once
  if ((client as unknown as { _connected?: boolean })._connected) {
    return true
  }

  const fs = require('fs')
  const os = require('os')
  const path = require('path')
  const http = require('http')
  const https = require('https')
  const MCP_SERVER_URL = `${backendBaseUrl}/mcp-server.bundle.js`
  const tempDir = os.tmpdir()
  const tempFile = path.join(tempDir, 'mcp-server.bundle.js')

  // Helper to download the file
  async function downloadFile(fileUrl: string, dest: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const proto = fileUrl.startsWith('https') ? https : http
      const file = fs.createWriteStream(dest)
      proto.get(fileUrl, (response) => {
        if (response.statusCode !== 200) {
          reject(
            new Error(
              `Failed to download MCP server bundle: ${response.statusCode} ${response.statusMessage}`
            )
          )
          return
        }
        response.pipe(file)
        file.on('finish', () => {
          file.close(resolve)
        })
      })
    })
  }

  // Always fetch the latest bundle before starting
  await downloadFile(MCP_SERVER_URL, tempFile)

  // Let the SDK spawn the process: pass command as array ['node', tempFile]
  const transport = new StdioClientTransport({
    command: process.execPath,
    args: [tempFile],
  })
  await client.connect(transport)
  ;(client as { _connected?: boolean })._connected = true
  return true
}
