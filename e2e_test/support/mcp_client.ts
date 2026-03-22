import { Client } from '@modelcontextprotocol/sdk/client/index.js'
import { StdioClientTransport } from '@modelcontextprotocol/sdk/client/stdio.js'
import path from 'path'
import fs from 'fs'
import { E2E_BACKEND_BASE_URL } from '../config/constants'

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
    accessToken,
  }: {
    baseUrl: string
    accessToken: string
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
    const mcpServerNodeModules = path.join(
      repoRoot,
      'mcp-server',
      'node_modules'
    )
    if (!fs.existsSync(bundlePath)) {
      throw new Error(
        `MCP server bundle not found at ${bundlePath}. Please build it first: \n  CURSOR_DEV=true nix develop -c pnpm mcp-server:bundle`
      )
    }
    // Let the SDK spawn the process: pass command as array ['node', bundlePath]
    // Set NODE_PATH so external packages (like @modelcontextprotocol/sdk and express) can be resolved
    const nodePath = process.env.NODE_PATH
      ? `${process.env.NODE_PATH}:${mcpServerNodeModules}`
      : mcpServerNodeModules
    const apiBaseUrl =
      baseUrl && baseUrl !== 'undefined' ? baseUrl : E2E_BACKEND_BASE_URL
    this.transport = new StdioClientTransport({
      command: process.execPath,
      args: [bundlePath],
      env: {
        ...process.env,
        NODE_PATH: nodePath,
        DOUGHNUT_API_BASE_URL: apiBaseUrl,
        DOUGHNUT_API_AUTH_TOKEN: accessToken,
      },
    })
    await this.client.connect(this.transport)
    ;(this.client as { _connected?: boolean })._connected = true
    return true
  }

  async callMcpToolWithParams({
    apiName,
    params,
  }: {
    apiName: string
    params: Record<string, any>
  }) {
    if (!this.client) throw new Error('MCP client is not connected')
    const result = await this.client.callTool({
      name: apiName,
      arguments: params,
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
