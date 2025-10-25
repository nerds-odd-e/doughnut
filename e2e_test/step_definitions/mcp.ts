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
  'AI agent searches for relevant notes using MCP tool with the term {string}',
  (searchTerm: string) => {
    cy.task('callMcpToolWithParams', {
      apiName: 'find_most_relevant_note',
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

Then(
  'the graph response should contain the focus note {string}',
  (noteTitle: string) => {
    cy.get('@MCPGraphResponse').then((response) => {
      const responseString = JSON.stringify(response)
      expect(responseString).to.contain('focusNote')
      expect(responseString).to.contain(noteTitle)
    })
  }
)

When(
  'AI agent extracts note ID and calls get graph MCP tool with token limit {string}',
  (limit: string) => {
    cy.get('@MCPApiResponse').then((searchResponse) => {
      const responseData = searchResponse as unknown as ApiResponse
      const responseText = responseData.content[0]?.text || ''
      const searchResult = JSON.parse(responseText)

      // Check if noteTopology exists and has id
      if (!(searchResult.noteTopology && searchResult.noteTopology.id)) {
        throw new Error(`Invalid search result structure: ${responseText}`)
      }

      const noteId = searchResult.noteTopology.id
      const tokenLimit = parseInt(limit)

      cy.task('callMcpToolWithParams', {
        apiName: 'get_note_graph',
        params: { noteId: noteId, tokenLimit: tokenLimit },
      }).then((graphResponse) => {
        cy.wrap(graphResponse).as('MCPGraphResponse')
      })
    })
  }
)

Then('the graph response should contain {string}', (expectedText: string) => {
  cy.get('@MCPGraphResponse').then((response) => {
    const actualResponse = response as unknown as ApiResponse
    const responseText = actualResponse.content[0]?.text || ''

    expect(responseText).to.contain(expectedText)
  })
})

Then(
  'the graph response should not contain {string}',
  (unexpectedText: string) => {
    cy.get('@MCPGraphResponse').then((response) => {
      const actualResponse = response as unknown as ApiResponse
      const responseText = actualResponse.content[0]?.text || ''

      expect(responseText).to.not.contain(unexpectedText)
    })
  }
)
