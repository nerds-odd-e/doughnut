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
    cy.task('connectMcpClient', baseUrl)
  }
)

// Use the literal API names directly from the feature file
When('I call the {string} MCP tool', (apiName: string) => {
  const baseUrl = Cypress.config('baseUrl')
  cy.task('callMcpTool', { apiName, baseUrl }).then((response) => {
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

// Step definition for updating a note title by id
When(
  'I update a note title with this id {string} to {string}',
  (noteId: string, newTitle: string) => {
    cy.task('updateNoteTitle', { noteId, newTitle }).then((response) => {
      cy.wrap(response).as('MCPApiResponse')
    })
  }
)

Then(
  'should receive a list of notebooks in the MCP response: {string}',
  (expectedResponse: string) => {
    cy.get('@MCPApiResponse').then((response) => {
      const expectedWithQuotes = `${expectedResponse}`
      const actualResponse = response as unknown as ApiResponse
      expect(actualResponse.content[0]!.text).to.equal(expectedWithQuotes)
    })
  }
)
