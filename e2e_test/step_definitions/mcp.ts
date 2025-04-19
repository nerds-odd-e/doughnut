import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'
import { getMcpClient, connectMcpClient } from '../start/mcp_client'

interface ApiResponse {
  content: Array<{
    text: string
  }>
  status: string
}

let apiResponse: ApiResponse

Given(
  'I connect to an MCP client that connects to Doughnut MCP service with my MCP token',
  () => {
    // First, we need to get the MCP token from the page
    cy.get('@savedTokenValue').then((token) => {
      if (token) {
        // Then connect with the token
        const asyncFunction = async () => {
          await connectMcpClient(token as unknown as string)
        }

        cy.wrap(asyncFunction())
      } else {
        throw new Error('MCP token is not found or empty')
      }
    })
  }
)

// Use the literal API names directly from the feature file
When('I call the {string} MCP tool', (apiName: string) => {
  const asyncFunction = async () => {
    const client = getMcpClient()

    const result = await client.callTool({
      name: apiName,
    })
    return result
  }

  cy.wrap(asyncFunction()).then((response) => {
    apiResponse = response as ApiResponse
  })
})

// Use the literal expected response directly from the feature file
Then('the response should contain {string}', (expectedResponse: string) => {
  const expectedWithQuotes = `"${expectedResponse}"`
  expect(apiResponse.content[0]!.text).to.equal(expectedWithQuotes)
})
