import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'

interface ApiResponse {
  content: Array<{
    text: string
  }>
  status: string
}

Given(
  'I connect to an MCP client that connects to Doughnut MCP service',
  () => {
    const baseUrl = Cypress.config('baseUrl')
    cy.get('@savedMcpToken').then((mcpToken) => {
      cy.task('spawnAndConnectMcpServer', { baseUrl, mcpToken })
    })
  }
)

// Use the literal API names directly from the feature file
When('I call the {string} MCP tool', (apiName: string) => {
  cy.task('callMcpTool', { apiName }).then((response) => {
    cy.wrap(response).as('MCPApiResponse')
  })
})

// Use the literal expected response directly from the feature file
Then('the response should contain {string}', (expectedResponse: string) => {
  cy.get('@MCPApiResponse').then((response) => {
    const actualResponse = response as unknown as ApiResponse
    expect(actualResponse.content[0]!.text).to.contain(expectedResponse)
  })
})
