import { Client } from '@modelcontextprotocol/sdk/client/index.js'
import { StdioClientTransport } from '@modelcontextprotocol/sdk/client/stdio.js'
import path from 'path'
import fs from 'fs'

interface MaybeChildProcess {
  child?: { kill: () => void }
}
interface MaybeDisconnect {
  disconnect?: () => Promise<void>
}

class McpClient {
  client: Client | null = null
  transport: StdioClientTransport | null = null

  async spawnAndConnectMcpServer({
    baseUrl,
    mcpToken,
  }: {
    baseUrl: string
    mcpToken: string
  }) {
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

    // Resolve local MCP server bundle built under mcp-server/dist relative to repo root
    const repoRoot = path.resolve(__dirname, '..', '..')
    const bundlePath = path.join(
      repoRoot,
      'mcp-server',
      'dist',
      'mcp-server.bundle.mjs'
    )
    if (!fs.existsSync(bundlePath)) {
      throw new Error(
        `MCP server bundle not found at ${bundlePath}. Please build it first: \n  CURSOR_DEV=true nix develop -c pnpm mcp-server:bundle`
      )
    }
    // Let the SDK spawn the process: pass command as array ['node', bundlePath]
    this.transport = new StdioClientTransport({
      command: process.execPath,
      args: [bundlePath],
      env: {
        DOUGHNUT_API_BASE_URL: baseUrl,
        DOUGHNUT_API_AUTH_TOKEN: mcpToken,
      },
    })
    await this.client.connect(this.transport)
    ;(this.client as { _connected?: boolean })._connected = true
    return true
  }

  async callMcpTool({ apiName }: { apiName: string }) {
    if (!this.client) throw new Error('MCP client is not connected')
    const result = await this.client.callTool({
      name: apiName,
    })
    return result
  }

  async callMcpToolWithArgs({
    apiName,
    args,
  }: {
    apiName: string
    args: string
  }) {
    if (!this.client) throw new Error('MCP client is not connected')
    const result = await this.client.callTool({
      name: apiName,
      arguments: { query: args },
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
