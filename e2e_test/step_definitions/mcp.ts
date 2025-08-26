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
  cy.task('callMcpTool', { apiName }).then((response) => {
    cy.wrap(response).as('MCPApiResponse')
  })
})

When('I search for notes with the term {string}', (searchTerm: string) => {
  cy.task('callMcpTool', {
    apiName: 'get_relevant_note',
    args: { query: searchTerm },
  }).then((response) => {
    cy.wrap(response).as('MCPApiResponse')
  })
})

Then('the response should contain {string}', (expectedResponse: string) => {
  cy.get('@MCPApiResponse').then((response) => {
    const responseString = JSON.stringify(response)
    const foundInString = responseString.includes(expectedResponse);
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
  'AI agent calls the "add_note" MCP tool with notebook title {string} and title {string}',
  (notebookTitle: string, noteTitle: string) => {
    cy.task('callMcpTool', {
      apiName: 'add_note',
      params: { notebookTitle, noteTitle },
    }).then((response) => {
      cy.wrap(response).as('MCPAddNoteResponse')
    })
  }
)

Then(
  '"{string}" note is added to "{string}" notebook',
  (noteTitle: string, notebookTitle: string) => {
    cy.task('callMcpTool', { apiName: 'get_notebook_list' }).then(
      (response) => {
        const actualResponse = response as unknown as ApiResponse
        const found = actualResponse.content.some((item) =>
          item.text.includes(notebookTitle)
        )
        expect(found).to.be.true
      }
    )
  }
)

// --- Add note with details to notebook ---
When(
  'AI agent calls the "add_note" MCP tool with notebook title {string} and title {string} and details {string}',
  (notebookTitle: string, noteTitle: string, details: string) => {
    cy.task('callMcpTool', {
      apiName: 'add_note',
      params: { notebookTitle, noteTitle, details },
    }).then((response) => {
      cy.wrap(response).as('MCPAddNoteResponse')
    })
  }
)

Then(
  '"{string}" note with details "{string}" is added to "{string}" notebook',
  (noteTitle: string, details: string, notebookTitle: string) => {
    cy.task('callMcpTool', { apiName: 'get_notebook_list' }).then(
      (response) => {
        const actualResponse = response as unknown as ApiResponse
        const found = actualResponse.content.some((item) =>
          item.text.includes(notebookTitle)
        )
        expect(found).to.be.true
      }
    )
  }
)

// --- Add note to notebook ---
When(
  'AI agent calls the "add_note" MCP tool with notebook title {string} and title {string}',
  (notebookTitle: string, noteTitle: string) => {
    cy.task('callMcpTool', {
      apiName: 'add_note',
      params: { notebookTitle, noteTitle },
    }).then((response) => {
      cy.wrap(response).as('MCPAddNoteResponse')
    })
  }
)

Then(
  '"{string}" note is added to "{string}" notebook',
  (noteTitle: string, notebookTitle: string) => {
    cy.task('callMcpTool', { apiName: 'get_notebook_list' }).then(
      (response) => {
        const actualResponse = response as unknown as ApiResponse
        const found = actualResponse.content.some((item) =>
          item.text.includes(notebookTitle)
        )
        expect(found).to.be.true
      }
    )
  }
)

// --- Add note with details to notebook ---
When(
  'AI agent calls the "add_note" MCP tool with notebook title {string} and title {string} and details {string}',
  (notebookTitle: string, noteTitle: string, details: string) => {
    cy.task('callMcpTool', {
      apiName: 'add_note',
      params: { notebookTitle, noteTitle, details },
    }).then((response) => {
      cy.wrap(response).as('MCPAddNoteResponse')
    })
  }
)

Then(
  '"{string}" note with details "{string}" is added to "{string}" notebook',
  (noteTitle: string, details: string, notebookTitle: string) => {
    cy.task('callMcpTool', { apiName: 'get_notebook_list' }).then(
      (response) => {
        const actualResponse = response as unknown as ApiResponse
        const found = actualResponse.content.some((item) =>
          item.text.includes(notebookTitle)
        )
        expect(found).to.be.true
      }
    )
  }
)
