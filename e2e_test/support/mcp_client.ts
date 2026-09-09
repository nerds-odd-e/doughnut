import { Client } from '@modelcontextprotocol/sdk/client/index.js'
import { StdioClientTransport } from '@modelcontextprotocol/sdk/client/stdio.js'
import path from 'path'
import fs from 'fs'
import { E2E_APP_BASE_URL } from '../config/constants'

class McpClient {
  client: Client | null = null
  transport: StdioClientTransport | null = null

  async spawnAndConnectMcpServer({
    baseUrl,
    accessToken,
    args,
  }: {
    baseUrl: string
    accessToken: string
    args?: string[]
  }) {
    if (this.client !== null) {
      throw new Error(
        'MCP client is already connected. Please disconnect the previous client before connecting a new one.'
      )
    }
    this.client = new Client(
      {
        name: 'donut-mcp-client',
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
    const serverArgs = args ?? [bundlePath]
    if (args === undefined && !fs.existsSync(bundlePath)) {
      throw new Error(
        `MCP server bundle not found at ${bundlePath}. Please build it first: \n  CURSOR_DEV=true nix develop -c pnpm mcp-server:bundle`
      )
    }
    // Set NODE_PATH so external packages (like @modelcontextprotocol/sdk and express) can be resolved
    const nodePath = process.env.NODE_PATH
      ? `${process.env.NODE_PATH}:${mcpServerNodeModules}`
      : mcpServerNodeModules
    const apiBaseUrl =
      baseUrl && baseUrl !== 'undefined' ? baseUrl : E2E_APP_BASE_URL
    this.transport = new StdioClientTransport({
      command: process.execPath,
      args: serverArgs,
      env: {
        ...process.env,
        NODE_PATH: nodePath,
        DONUT_API_BASE_URL: apiBaseUrl,
        DONUT_API_AUTH_TOKEN: accessToken,
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
    await this.client?.close()
    this.client = null
    this.transport = null
    return true
  }
}

export default new McpClient()
