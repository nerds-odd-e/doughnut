import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'
import { getMcpClient, connectMcpClient } from '../../start/mcp_client'

interface InstructionResponse {
  content: Array<{
    text: string
  }>
  status: string
}

let instructionResponse: InstructionResponse

Given(
  'I connect to an MCP client that connects to Doughnut MCP service',
  () => {
    const asyncFunction = async () => {
      await connectMcpClient()
    }

    cy.wrap(asyncFunction())
  }
)

When('Call instruction API by MCP Client', () => {
  const asyncFunction = async () => {
    const client = getMcpClient()
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
