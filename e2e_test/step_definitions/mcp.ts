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
  const baseUrl = Cypress.config('baseUrl')
  cy.get('@savedMcpToken').then((mcpToken) => {
    cy.task('callMcpTool', { apiName, baseUrl, mcpToken }).then((response) => {
      cy.wrap(response).as('MCPApiResponse')
    })
  })
})

// Use the literal expected response directly from the feature file
Then('the response should equal {string}', (expectedResponse: string) => {
  cy.get('@MCPApiResponse').then((response) => {
    const expectedWithQuotes = `${expectedResponse}`
    const actualResponse = response as unknown as ApiResponse
    expect(actualResponse.content[0]!.text).to.equal(expectedWithQuotes)
  })
})

Then(
  'I should receive a list of notebooks in the MCP response contain {string}',
  (expectedResponse: string) => {
    cy.get('@MCPApiResponse').then((response) => {
      const expectedWithQuotes = `${expectedResponse}`
      const actualResponse = response as unknown as ApiResponse
      expect(actualResponse.content[0]!.text).to.equal(expectedWithQuotes)
    })
  }
)

// step definition for get_user_info API
When('call Mcp server get_user_info API', () => {
  const apiName = 'get_user_info'
  const baseUrl = Cypress.config('baseUrl')
  cy.get('@savedMcpToken').then((mcpToken) => {
    cy.task('callMcpTool', { apiName, baseUrl, mcpToken }).then((response) => {
      cy.wrap(response).as('MCPApiResponse')
    })
  })
})

Then(
  'the response should return user name contain {string}',
  (expectedResponse: string) => {
    cy.get('@MCPApiResponse').then((response) => {
      const expectedWithQuotes = `${expectedResponse}`
      const actualResponse = response as unknown as ApiResponse
      expect(actualResponse.content[0]!.text).to.contain(expectedWithQuotes)
    })
  }
)
