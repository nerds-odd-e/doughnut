import { Client } from '@modelcontextprotocol/sdk/client/index.js'
import { StdioClientTransport } from '@modelcontextprotocol/sdk/client/stdio.js'
import fs from 'fs'
import os from 'os'
import path from 'path'
import http from 'http'
import https from 'https'

interface MaybeChildProcess {
  child?: { kill: () => void }
}
interface MaybeDisconnect {
  disconnect?: () => Promise<void>
}

class McpClient {
  client: Client | null = null
  transport: StdioClientTransport | null = null

  async #downloadFile(fileUrl: string, dest: string): Promise<void> {
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
          file.close(() => resolve())
        })
      })
    })
  }

  async spawnAndConnectMcpServer({
    baseUrl,
    mcpToken,
  }: { baseUrl: string; mcpToken: string }) {
    if (this.client !== null) {
      throw new Error(
        'MCP client is already connected. Please disconnect the previous client before connecting a new one.'
      )
    }
    this.client = new Client(
      {
        name: 'doughnut-mcp-client',
        version: '1.0.0',
      },
      {
        capabilities: {},
      }
    )

    const MCP_SERVER_URL = `${baseUrl}/mcp-server.bundle.mjs`
    const tempDir = os.tmpdir()
    const tempFile = path.join(tempDir, 'mcp-server.bundle.mjs')

    // Always fetch the latest bundle before starting
    await this.#downloadFile(MCP_SERVER_URL, tempFile)
    // Let the SDK spawn the process: pass command as array ['node', tempFile, mcpToken]
    this.transport = new StdioClientTransport({
      command: process.execPath,
      args: [tempFile, mcpToken],
      env: {
        DOUGHNUT_API_BASE_URL: 'http://localhost:9081',
      },
    })
    await this.client.connect(this.transport)
    ;(this.client as { _connected?: boolean })._connected = true
    return true
  }

  async callMcpTool({
    apiName,
  }: {
    apiName: string
  }) {
    if (!this.client) throw new Error('MCP client is not connected')
    const result = await this.client.callTool({
      name: apiName,
    })
    return result
  }

  async disconnectMcpServer() {
    if (
      this.transport &&
      (this.transport as MaybeChildProcess).child &&
      typeof (this.transport as MaybeChildProcess).child!.kill === 'function'
    ) {
      ;(this.transport as MaybeChildProcess).child!.kill()
    }
    this.transport = null

    if (
      this.client &&
      typeof (this.client as MaybeDisconnect).disconnect === 'function'
    ) {
      await (this.client as MaybeDisconnect).disconnect!()
    }
    this.client = null
    return true
  }
}

export default new McpClient()
