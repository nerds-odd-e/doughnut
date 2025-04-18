import { When, Then, Given } from '@badeball/cypress-cucumber-preprocessor'
import { getMcpClient, connectMcpClient } from '../../start/mcp_client'

interface UsernameResponse {
  content: Array<{
    text: string
  }>
  status: string
}

let usernameResponse: UsernameResponse

Given('User have valid MCP token', () => {
  const asyncFunction = async () => {
    await connectMcpClient()
  }

  cy.wrap(asyncFunction())
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
    usernameResponse = response as UsernameResponse
  })
})

Then('Return username', () => {
  expect(usernameResponse.content[0]!.text).to.equal('"Terry"')
})
