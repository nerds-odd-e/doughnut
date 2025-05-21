import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'
import { getMcpClient, connectMcpClient } from '../start/mcp_client'


interface ApiResponse {
  content: Array<{
    text: string
  }>
  status: string
}


When('the client requests user information via MCP service', () => {
  const asyncFunction = async () => {
    const client = getMcpClient()
    const result = await client.callTool({
      name: 'getUserInfo',
      arguments: { mcpToken: cy.get('@savedMcpToken') }
    })
    return result
  }
  cy.wrap(asyncFunction()).then((response) => {
    cy.wrap(response).as('MCPApiResponse')
  })
})

Then('the {string} should be returned', (expectedResponse: string) => {
  cy.get('@MCPApiResponse').then((response) => {
    const expectedWithQuotes = `"${expectedResponse}"`
    const actualResponse = response as unknown as ApiResponse
    expect(actualResponse.content[0]!.text).to.equal(expectedWithQuotes)
  })
})
