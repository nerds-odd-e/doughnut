import { When, Then } from '@badeball/cypress-cucumber-preprocessor'

interface ApiResponse {
  content: Array<{
    text: string
  }>
  status: string
}

When('call Mcp server get_user_info API', () => {
  const apiName = 'get_user_info'
  const baseUrl = Cypress.config('backendBaseUrl')
  cy.task('callMcpTool', { apiName, baseUrl }).then((response) => {
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
