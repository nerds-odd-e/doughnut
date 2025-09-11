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

// --- Get graph workflow ---
When(
  'AI agent extracts note ID from the search result and calls get graph MCP tool',
  () => {
    cy.get('@MCPApiResponse').then((searchResponse) => {
      const responseData = searchResponse as unknown as ApiResponse
      const responseText = responseData.content[0].text
      const searchResult = JSON.parse(responseText)

      // Check if noteTopology exists and has id
      if (!(searchResult.noteTopology && searchResult.noteTopology.id)) {
        throw new Error(`Invalid search result structure: ${responseText}`)
      }

      const noteId = searchResult.noteTopology.id

      cy.task('callMcpToolWithParams', {
        apiName: 'get_graph_with_note_id',
        params: { noteId: noteId },
      }).then((graphResponse) => {
        cy.wrap(graphResponse).as('MCPGraphResponse')
      })
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

Then('the graph response should contain related notes', () => {
  cy.get('@MCPGraphResponse').then((response) => {
    const actualResponse = response as unknown as ApiResponse
    const responseText = actualResponse.content[0].text
    const graphResult = JSON.parse(responseText)

    expect(graphResult).to.have.property('relatedNotes')
    expect(graphResult.relatedNotes).to.be.an('array')
    expect(graphResult.relatedNotes.length).to.be.greaterThan(0)

    // Verify the related notes contain the expected content from our test scenario
    const relatedNoteTitles = graphResult.relatedNotes.map(
      (note: any) => note.title
    )

    // Should contain the parent note
    expect(relatedNoteTitles).to.include('Programming Concepts')

    // Should contain the child notes
    expect(relatedNoteTitles).to.include('Classes')
    expect(relatedNoteTitles).to.include('Inheritance')

    // Should contain the sibling note
    expect(relatedNoteTitles).to.include('Functional')
  })
})
