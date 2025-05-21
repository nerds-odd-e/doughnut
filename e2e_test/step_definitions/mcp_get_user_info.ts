import { When, Then } from '@badeball/cypress-cucumber-preprocessor'

interface ApiResponse {
  content: Array<{
    text: string
  }>
  status: string
}

When('the client requests user information via MCP service', () => {
  const apiName = 'getUserInfo'
  const backendBaseUrl =
    Cypress.env('backendBaseUrl') || 'http://localhost:9081'
  cy.task('callMcpTool', { apiName, backendBaseUrl }).then((response) => {
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
