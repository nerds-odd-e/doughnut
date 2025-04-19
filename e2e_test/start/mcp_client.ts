import { Client } from '@modelcontextprotocol/sdk/client/index.js'
import { SSEClientTransport } from '@modelcontextprotocol/sdk/client/sse.js'

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

export const connectMcpClient = async (mcpToken: string) => {
  const client = getMcpClient()
  let sseUrl = `${Cypress.config().backendBaseUrl}/sse`

  sseUrl = `${sseUrl}?token=${mcpToken}`

  const transport = new SSEClientTransport(new URL(sseUrl))
  await client.connect(transport)
  return client
}
