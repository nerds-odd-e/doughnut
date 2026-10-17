import { e2eAppBaseUrl } from '../../support/e2eAppUrl'

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
      cy.task('disconnectMcpServer')
      cy.get('@savedAccessToken').then((accessToken) => {
        cy.task('spawnAndConnectMcpServer', {
          baseUrl: e2eAppBaseUrl(),
          accessToken,
        })
      })
      return this
    },

    callTool(apiName: string, params: Record<string, unknown> = {}) {
      cy.task('callMcpToolWithParams', { apiName, params }).then((response) => {
        cy.wrap(response).as('MCPApiResponse')
      })
      return this
    },

    listNoteCandidates(searchTerm: string) {
      return this.callTool('list_note_candidates', { query: searchTerm })
    },

    getNoteGraphFromLastSearch(tokenLimit: number) {
      getResponseText('@MCPApiResponse').then((responseText) => {
        const noteId = JSON.parse(responseText)[0].noteId

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
        expect(
          responseText,
          `Expected MCP search response to contain "${expectedText}", but found: ${responseText}`
        ).to.contain(expectedText)
      })
      return this
    },

    expectCandidateTitles(titles: string[]) {
      getResponseText('@MCPApiResponse').then((responseText) => {
        expect(
          JSON.parse(responseText).map((c: { title: string }) => c.title)
        ).to.deep.equal(titles)
      })
      return this
    },

    expectGraphContainsFocusNote(noteTitle: string) {
      cy.get('@MCPGraphResponse').then((response) => {
        const responseString = JSON.stringify(response)
        expect(
          responseString,
          `Expected MCP graph to include focusNote, but found: ${responseString}`
        ).to.contain('focusNote')
        expect(
          responseString,
          `Expected MCP graph focus note to contain "${noteTitle}", but found: ${responseString}`
        ).to.contain(noteTitle)
      })
      return this
    },

    expectGraphContains(expectedText: string) {
      getResponseText('@MCPGraphResponse').then((responseText) => {
        expect(
          responseText,
          `Expected MCP graph to contain "${expectedText}", but found: ${responseText}`
        ).to.contain(expectedText)
      })
      return this
    },

    expectGraphNotContains(unexpectedText: string) {
      getResponseText('@MCPGraphResponse').then((responseText) => {
        expect(
          responseText,
          `Expected MCP graph not to contain "${unexpectedText}", but found: ${responseText}`
        ).to.not.contain(unexpectedText)
      })
      return this
    },
  }
}
