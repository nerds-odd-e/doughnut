import { Client } from '@modelcontextprotocol/sdk/client/index.js'
import { StdioClientTransport } from '@modelcontextprotocol/sdk/client/stdio.js'

let client: Client | null = null
let transport: StdioClientTransport | null = null

interface MaybeChildProcess {
  child?: { kill: () => void }
}
interface MaybeDisconnect {
  disconnect?: () => Promise<void>
}

/**
 * Spawns the MCP server as a child process and connects a new MCP client to it.
 * Stores the client instance in a module-scoped variable.
 */
export const spawnAndConnectMcpServer = async ({
  baseUrl,
  mcpToken,
}: { baseUrl: string; mcpToken: string }) => {
  client = new Client(
    {
      name: 'doughnut-mcp-client',
      version: '1.0.0',
    },
    {
      capabilities: {},
    }
  )

  const fs = require('fs')
  const os = require('os')
  const path = require('path')
  const http = require('http')
  const https = require('https')
  const MCP_SERVER_URL = `${baseUrl}/mcp-server.bundle.mjs`
  const tempDir = os.tmpdir()
  const tempFile = path.join(tempDir, 'mcp-server.bundle.mjs')

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
  transport = new StdioClientTransport({
    command: process.execPath,
    args: [tempFile],
    env: {
      DOUGHNUT_API_BASE_URL: 'http://localhost:9081',
      DOUGHNUT_API_AUTH_TOKEN: mcpToken,
    },
  })
  await client.connect(transport)
  ;(client as { _connected?: boolean })._connected = true
  return true
}

export const callMcpTool = async ({
  apiName,
  baseUrl,
  mcpToken,
}: {
  apiName: string
  baseUrl: string
  mcpToken: string
}) => {
  if (!client) throw new Error('MCP client is not connected')
  const result = await client.callTool({
    name: apiName,
    baseUrl: baseUrl,
    mcpToken: mcpToken,
  })
  return result
}

export const disconnectMcpServer = async () => {
  if (
    transport &&
    (transport as MaybeChildProcess).child &&
    typeof (transport as MaybeChildProcess).child!.kill === 'function'
  ) {
    ;(transport as MaybeChildProcess).child!.kill()
  }
  transport = null

  if (client && typeof (client as MaybeDisconnect).disconnect === 'function') {
    await (client as MaybeDisconnect).disconnect!()
  }
  client = null
  return true
}
