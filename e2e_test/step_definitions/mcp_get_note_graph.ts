import { When, Then } from '@badeball/cypress-cucumber-preprocessor'

interface ApiResponse {
  content: Array<{
    text: string
  }>
  status: string
}

When(
  'the client requests read note with graph from {string} via MCP service',
  (noteId: string) => {
    const apiName = 'get_graph_with_note_id'
    const baseUrl = Cypress.config('backendBaseUrl')

    cy.task('callMcpTool', { apiName, baseUrl, noteId }).then((response) => {
      cy.wrap(response).as('MCPApiResponse')
    })
  }
)

Then('the {string} should be returned', (expectedResponse: string) => {
  cy.get('@MCPApiResponse').then((response) => {
    const expectedWithQuotes = `"${expectedResponse}"`
    const actualResponse = response as unknown as ApiResponse
    expect(actualResponse.content[0]!.text).to.contains(expectedWithQuotes)
  })
})
