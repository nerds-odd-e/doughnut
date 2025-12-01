interface ApiResponse {
  content: Array<{
    text: string
    type?: string
  }>
  status: string
}

const getResponseText = (alias: string) => {
  return cy.get(alias).then((response) => {
    const apiResponse = response as unknown as ApiResponse
    return apiResponse.content[0]?.text || ''
  })
}

export const mcpAgentActions = () => {
  return {
    connect() {
      const baseUrl = Cypress.config('baseUrl')
      cy.get('@savedMcpToken').then((mcpToken) => {
        cy.task('spawnAndConnectMcpServer', { baseUrl, mcpToken })
      })
      return this
    },

    callTool(apiName: string, params: Record<string, unknown> = {}) {
      cy.task('callMcpToolWithParams', { apiName, params }).then((response) => {
        cy.wrap(response).as('MCPApiResponse')
      })
      return this
    },

    searchForNote(searchTerm: string) {
      cy.task('callMcpToolWithParams', {
        apiName: 'find_most_relevant_note',
        params: { query: searchTerm },
      }).then((response) => {
        cy.wrap(response).as('MCPApiResponse')
      })
      return this
    },

    addNote(parentTitle: string, newTitle: string) {
      cy.task('callMcpToolWithParams', {
        apiName: 'add_note',
        params: { parentTitle, newTitle },
      }).then((response) => {
        cy.wrap(response).as('MCPAddNoteResponse')
      })
      return this
    },

    getNoteGraphFromLastSearch(tokenLimit: number) {
      cy.get('@MCPApiResponse').then((searchResponse) => {
        const responseData = searchResponse as unknown as ApiResponse
        const responseText = responseData.content[0]?.text || ''
        const searchResult = JSON.parse(responseText)

        // Check if noteTopology exists and has id
        if (!(searchResult.noteTopology && searchResult.noteTopology.id)) {
          throw new Error(`Invalid search result structure: ${responseText}`)
        }

        const noteId = searchResult.noteTopology.id

        cy.task('callMcpToolWithParams', {
          apiName: 'get_note_graph',
          params: { noteId, tokenLimit },
        }).then((graphResponse) => {
          cy.wrap(graphResponse).as('MCPGraphResponse')
        })
      })
      return this
    },

    // Response validation methods
    expectResponseContains(expectedText: string) {
      getResponseText('@MCPApiResponse').then((responseText) => {
        expect(responseText).to.contain(expectedText)
      })
      return this
    },

    expectSearchResultIncludesNoteTitle(noteTitle: string) {
      cy.get('@MCPApiResponse').then((response) => {
        const actualResponse = response as unknown as ApiResponse
        const found = actualResponse.content.some((item) =>
          item.text.includes(noteTitle)
        )
        expect(found).to.be.true
      })
      return this
    },

    expectGraphContainsFocusNote(noteTitle: string) {
      cy.get('@MCPGraphResponse').then((response) => {
        const responseString = JSON.stringify(response)
        expect(responseString).to.contain('focusNote')
        expect(responseString).to.contain(noteTitle)
      })
      return this
    },

    expectGraphContains(expectedText: string) {
      getResponseText('@MCPGraphResponse').then((responseText) => {
        expect(responseText).to.contain(expectedText)
      })
      return this
    },

    expectGraphNotContains(unexpectedText: string) {
      getResponseText('@MCPGraphResponse').then((responseText) => {
        expect(responseText).to.not.contain(unexpectedText)
      })
      return this
    },
  }
}
