import { e2eAppBaseUrl } from '../../support/e2eAppUrl'
import { NOTE_CONTEXT_MCP_TOOLS } from '../../support/mcp_note_context_tools'

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

const callToolOnFirstCandidate = (
  apiName: string,
  params: Record<string, unknown>,
  alias: string
) => {
  getResponseText('@MCPApiResponse').then((responseText) => {
    const noteId = JSON.parse(responseText)[0].noteId
    cy.task('callMcpToolWithParams', {
      apiName,
      params: { noteId, ...params },
    }).then((response) => {
      cy.wrap(response).as(alias)
    })
  })
}

const getReadRevision = () =>
  getResponseText('@MCPReadResponse').then((responseText) => {
    const { acceptedRevision } = JSON.parse(responseText)
    expect(acceptedRevision).to.match(/^[0-9a-f]{40}$/)
    return acceptedRevision as string
  })

export const mcpAgentActions = () => {
  return {
    connect() {
      cy.task('disconnectMcp')
      cy.get('@savedAccessToken').then((accessToken) => {
        cy.task('connectMcpHttps', {
          baseUrl: e2eAppBaseUrl(),
          accessToken,
        })
      })
      return this
    },

    expectDiscoveredNoteContextTools() {
      cy.task<{ tools: string[] | null }>('mcpClientConnectionInfo').then(
        (info) => {
          expect(
            info.tools,
            'MCP tools discovered over verified HTTPS'
          ).to.include.members([...NOTE_CONTEXT_MCP_TOOLS])
        }
      )
      return this
    },

    expectEqualTitleCandidatesDisambiguated() {
      getResponseText('@MCPApiResponse').then((responseText) => {
        const candidates = JSON.parse(responseText) as Array<{
          title: string
          notebookName: string
          folderName?: string
        }>
        expect(candidates, 'equal-title candidate count').to.have.length(2)
        expect(candidates.map((c) => c.title)).to.deep.equal([
          'Functional',
          'Functional',
        ])
        const byNotebook = Object.fromEntries(
          candidates.map((c) => [c.notebookName, c])
        )
        expect(byNotebook['CS concepts']?.folderName).to.equal(
          'Programming Concepts'
        )
        expect(byNotebook.Maths?.folderName).to.equal(undefined)
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
      callToolOnFirstCandidate(
        'get_note_graph',
        { tokenLimit },
        'MCPGraphResponse'
      )
      return this
    },

    readNoteFromLastSearch() {
      callToolOnFirstCandidate('read_note', {}, 'MCPReadResponse')
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

    expectReadContentContains(content: string) {
      getResponseText('@MCPReadResponse').then((responseText) => {
        expect(JSON.parse(responseText).content).to.contain(content)
      })
      return this
    },

    rememberReadRevision() {
      getReadRevision().then((acceptedRevision) => {
        cy.wrap(acceptedRevision).as('citedRevision')
      })
      return this
    },

    expectReadRevisionDiffersFromCited() {
      cy.get<string>('@citedRevision').then((citedRevision) => {
        getReadRevision().then((acceptedRevision) => {
          expect(acceptedRevision).to.not.equal(citedRevision)
        })
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
