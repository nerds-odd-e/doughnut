import { Client } from '@modelcontextprotocol/sdk/client/index.js'
import { StreamableHTTPClientTransport } from '@modelcontextprotocol/sdk/client/streamableHttp.js'
import { startMcpTlsProxy, type McpTlsProxy } from './mcp_tls_proxy'
import { MCP_HTTP_PATH, NOTE_CONTEXT_MCP_TOOLS } from './mcp_note_context_tools'
import { E2E_APP_BASE_URL } from '../config/constants'

class McpClient {
  private client: Client | null = null
  private transport: StreamableHTTPClientTransport | null = null
  private tlsProxy: McpTlsProxy | null = null
  private lastMcpUrl: string | null = null
  private lastForwardOrigin: string | null = null
  private lastDiscoveredTools: string[] | null = null

  async connectMcpHttps({
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
    const forwardOrigin =
      baseUrl && baseUrl !== 'undefined' ? baseUrl : E2E_APP_BASE_URL
    this.tlsProxy = await startMcpTlsProxy(forwardOrigin)
    const mcpUrl = `${this.tlsProxy.httpsOrigin}${MCP_HTTP_PATH}`

    this.client = new Client(
      {
        name: 'donut-mcp-client',
        version: '1.0.0',
      },
      {
        capabilities: {},
      }
    )
    this.transport = new StreamableHTTPClientTransport(new URL(mcpUrl), {
      requestInit: {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
      fetch: this.tlsProxy.trustedFetch,
    })
    await this.client.connect(this.transport)
    const listed = await this.client.listTools()
    const toolNames = listed.tools.map((tool) => tool.name)
    for (const required of NOTE_CONTEXT_MCP_TOOLS) {
      if (!toolNames.includes(required)) {
        throw new Error(
          `MCP tools/list missing ${required}; discovered: ${toolNames.join(', ') || '(none)'}`
        )
      }
    }
    this.lastMcpUrl = mcpUrl
    this.lastForwardOrigin = forwardOrigin
    this.lastDiscoveredTools = toolNames
    return {
      mcpUrl: this.lastMcpUrl,
      forwardOrigin: this.lastForwardOrigin,
      tools: this.lastDiscoveredTools,
    }
  }

  async callMcpToolWithParams({
    apiName,
    params,
  }: {
    apiName: string
    params: Record<string, unknown>
  }) {
    if (!this.client) throw new Error('MCP client is not connected')
    return await this.client.callTool({
      name: apiName,
      arguments: params,
    })
  }

  connectionInfo() {
    return {
      mcpUrl: this.lastMcpUrl,
      forwardOrigin: this.lastForwardOrigin,
      tools: this.lastDiscoveredTools,
    }
  }

  async disconnectMcp() {
    try {
      await this.client?.close()
    } finally {
      this.client = null
      this.transport = null
      const proxy = this.tlsProxy
      this.tlsProxy = null
      if (proxy) {
        await proxy.close()
      }
    }
  }
}

export default new McpClient()
