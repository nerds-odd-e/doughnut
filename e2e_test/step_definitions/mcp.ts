import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'

interface ApiResponse {
  content: Array<{
    text: string
    type?: string
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
When('AI agent calls the {string} MCP tool', (apiName: string) => {
  cy.task('callMcpToolWithParams', { apiName, params: {} }).then((response) => {
    cy.wrap(response).as('MCPApiResponse')
  })
})

When(
  'AI agent searchs for relevant notes using MCP tool with the term {string}',
  (searchTerm: string) => {
    cy.task('callMcpToolWithParams', {
      apiName: 'get_relevant_note',
      params: { query: searchTerm },
    }).then((response) => {
      cy.wrap(response).as('MCPApiResponse')
    })
  }
)

Then('the response should contain {string}', (expectedResponse: string) => {
  cy.get('@MCPApiResponse').then((response) => {
    const responseString = JSON.stringify(response)
    const foundInString = responseString.includes(expectedResponse)
    expect(foundInString).to.be.true
  })
})

// Search-related steps
Then(
  'the search results should include a note with the title {string}',
  (noteTitle: string) => {
    cy.get('@MCPApiResponse').then((response) => {
      const actualResponse = response as unknown as ApiResponse
      const found = actualResponse.content.some((item) =>
        item.text.includes(noteTitle)
      )
      expect(found).to.be.true
    })
  }
)

// --- Add note to notebook ---
When(
  'AI agent adds note via MCP tool to add note {string} under {string}',
  (noteTitle: string, parentTitle: string) => {
    cy.task('callMcpToolWithParams', {
      apiName: 'add_note',
      params: { parentTitle: parentTitle, newTitle: noteTitle },
    }).then((response) => {
      cy.wrap(response).as('MCPAddNoteResponse')
    })
  }
)
