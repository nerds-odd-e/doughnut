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
  'I connect to an MCP client that connects to Doughnut MCP service',
  () => {
    const asyncFunction = async () => {
      await connectMcpClient()
    }

    cy.wrap(asyncFunction())
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

Given('User have valid MCP token', () => {
  const asyncFunction = async () => {
    await connectMcpClient()
  }

  cy.wrap(asyncFunction())
})
