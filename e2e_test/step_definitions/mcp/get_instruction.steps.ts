import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'
import { Client } from '@modelcontextprotocol/sdk/client/index.js'
import { SSEClientTransport } from '@modelcontextprotocol/sdk/client/sse.js'

interface InstructionResponse {
  content: Array<{
    text: string
  }>
  status: string
}

let client: Client
let instructionResponse: InstructionResponse

Given('MCP server is running', () => {
  client = new Client(
    {
      name: 'doughnut-mcp-client',
      version: '1.0.0',
    },
    {
      capabilities: {},
    }
  )

  const transport = new SSEClientTransport(
    new URL(`${Cypress.config().backendBaseUrl}/sse`)
  )

  const asyncFunction = async () => {
    await client.connect(transport)
  }

  cy.wrap(asyncFunction())
})

When('Call instruction API by MCP Client', () => {
  const asyncFunction = async () => {
    const result = await client.callTool({
      name: 'getInstruction',
    })
    return result
  }

  cy.wrap(asyncFunction()).then((response) => {
    instructionResponse = response as InstructionResponse
  })
})

Then('Return Doughnut instruction', () => {
  expect(instructionResponse.content[0]!.text).to.equal(
    '"Doughnut is a Personal Knowledge Management tool"'
  )
})
