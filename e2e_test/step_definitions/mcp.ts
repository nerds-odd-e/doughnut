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

// Handle both API names with separate step definitions
When('Call instruction tool by MCP Client', () => {
  const asyncFunction = async () => {
    const client = getMcpClient()
    const result = await client.callTool({
      name: 'getInstruction',
    })
    return result
  }

  cy.wrap(asyncFunction()).then((response) => {
    apiResponse = response as ApiResponse
  })
})

When('Call get username tool by MCP Client', () => {
  const asyncFunction = async () => {
    const client = getMcpClient()
    const result = await client.callTool({
      name: 'getUsername',
    })
    return result
  }

  cy.wrap(asyncFunction()).then((response) => {
    apiResponse = response as ApiResponse
  })
})

// Separate step definitions for each return value
Then('Return Doughnut instruction', () => {
  expect(apiResponse.content[0]!.text).to.equal(
    '"Doughnut is a Personal Knowledge Management tool"'
  )
})

Then('Return username', () => {
  expect(apiResponse.content[0]!.text).to.equal('"Terry"')
})

Given('User have valid MCP token', () => {
  const asyncFunction = async () => {
    await connectMcpClient()
  }

  cy.wrap(asyncFunction())
})
