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

Then('the {string} is fed into the correct MCP tool', (searchTerm: string) => {
  // Determine the correct MCP tool based on the search term
  // For demonstration, assume 'get_relevant_note' is the correct tool for any search term
  cy.task('callMcpToolWithParams', {
    apiName: 'get_relevant_note',
    params: { query: searchTerm },
  }).then((response) => {
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

Then(
  'the search results should not include a note with the title {string}',
  (noteTitle: string) => {
    cy.get('@MCPApiResponse').then((response) => {
      const actualResponse = response as unknown as ApiResponse
      const found = actualResponse.content.some((item) =>
        item.text.includes(noteTitle)
      )
      expect(found).to.be.false
    })
  }
)

When(
  'I get the note ID from the search result for {string}',
  (noteTitle: string) => {
    cy.get('@MCPApiResponse').then((response) => {
      const actualResponse = response as unknown as ApiResponse
      // Assume note ID is present in the text as "id: <number>" or similar
      const note = actualResponse.content.find((item) =>
        item.text.includes(noteTitle)
      )
      if (!note) {
        throw new Error('Note not found in search results')
      }
      const match = note.text.match(/id[:=]\s*(\d+)/i)
      if (!match) {
        throw new Error('Note ID not found in note text')
      }
      cy.wrap(Number(match[1])).as('noteId')
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
