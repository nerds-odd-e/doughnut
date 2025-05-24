import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

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

Given('I have a note with the id {int}', (nodeId: number) => {
  const notes = [{ Title: `Note ${nodeId}` }]
  start.testability().injectNotes(notes)
})

// Use the literal API names directly from the feature file
When('I call the {string} MCP tool', (apiName: string) => {
  const baseUrl = Cypress.config('baseUrl')
  cy.get('@savedMcpToken').then((mcpToken) => {
    cy.task('callMcpTool', { apiName, baseUrl, mcpToken }).then((response) => {
      cy.wrap(response).as('MCPApiResponse')
    })
  })
})

// Use the literal expected response directly from the feature file
Then('the response should equal {string}', (expectedResponse: string) => {
  cy.get('@MCPApiResponse').then((response) => {
    const expectedWithQuotes = `${expectedResponse}`
    const actualResponse = response as unknown as ApiResponse
    expect(actualResponse.content[0]!.text).to.equal(expectedWithQuotes)
  })
})

Then(
  'I should receive a list of notebooks in the MCP response contain {string}',
  (expectedResponse: string) => {
    cy.get('@MCPApiResponse').then((response) => {
      const expectedWithQuotes = `${expectedResponse}`
      const actualResponse = response as unknown as ApiResponse
      expect(actualResponse.content[0]!.text).to.equal(expectedWithQuotes)
    })
  }
)

// step definition for get_user_info API
When('call Mcp server get_user_info API', () => {
  const apiName = 'get_user_info'
  const baseUrl = Cypress.config('baseUrl')
  cy.get('@savedMcpToken').then((mcpToken) => {
    cy.task('callMcpTool', { apiName, baseUrl, mcpToken }).then((response) => {
      cy.wrap(response).as('MCPApiResponse')
    })
  })
})

Then(
  'the response should return user name contain {string}',
  (expectedResponse: string) => {
    cy.get('@MCPApiResponse').then((response) => {
      const expectedWithQuotes = `${expectedResponse}`
      const actualResponse = response as unknown as ApiResponse
      expect(actualResponse.content[0]!.text).to.contain(expectedWithQuotes)
    })
  }
)

// step definition for get_note_graph API
When(
  'the client requests read note with graph from {string} via MCP service',
  (noteId: string) => {
    const apiName = 'get_graph_with_note_id'
    const baseUrl = Cypress.config('baseUrl')
    cy.get('@savedMcpToken').then((mcpToken) => {
      cy.task('callMcpToolWithNoteId', {
        apiName,
        baseUrl,
        noteId,
        mcpToken,
      }).then((response) => {
        cy.wrap(response).as('MCPApiResponse')
      })
    })
  }
)

Then(
  'the response should return a json object contain {string}',
  (expectedResponse: string) => {
    cy.get('@MCPApiResponse').then((response) => {
      const expectedWithQuotes = `${expectedResponse}`
      const actualResponse = response as unknown as ApiResponse
      expect(actualResponse.content[0]!.text).to.contains(expectedWithQuotes)
    })
  }
)
