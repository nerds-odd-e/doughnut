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

export const connectMcpClient = async (mcpToken?: string) => {
  const client = getMcpClient()
  const config = Cypress.config() as any
  let sseUrl = `${config.backendBaseUrl}/sse`

  // If token is provided, add it to the URL
  if (mcpToken) {
    const token = mcpToken.replace('saved mcp token: ', '')
    sseUrl = `${sseUrl}?token=${token}`
  }

  const transport = new SSEClientTransport(
    // @ts-ignore
    new URL(sseUrl)
  )
  await client.connect(transport)
  return client
}
