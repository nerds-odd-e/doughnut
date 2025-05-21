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
    const backendBaseUrl =
      Cypress.config('backendBaseUrl') || 'http://localhost:9081'
    cy.task('connectMcpClient', backendBaseUrl)
    cy.wait(500)
  }
)

// Use the literal API names directly from the feature file
When('I call the {string} MCP tool', (apiName: string) => {
  const backendBaseUrl =
    Cypress.env('backendBaseUrl') || 'http://localhost:9081'
  cy.task('callMcpTool', { apiName, backendBaseUrl }).then((response) => {
    cy.wrap(response).as('MCPApiResponse')
  })
})

// Use the literal expected response directly from the feature file
Then('the response should contain {string}', (expectedResponse: string) => {
  cy.get('@MCPApiResponse').then((response) => {
    const expectedWithQuotes = `${expectedResponse}`
    const actualResponse = response as unknown as ApiResponse
    expect(actualResponse.content[0]!.text).to.equal(expectedWithQuotes)
  })
})
