import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'
import {
  WORKTREE_BROWSER_ISOLATION_EXPOSE_KEY,
  WORKTREE_RESET_ISOLATION_TASK_TIMEOUT_MS,
} from '../../scripts/worktree-isolation-constants.mjs'
import start from '../start'

type McpIsolationProof = {
  own: string
  foreign: string
  barrierDir: string
}

type McpConnectionInfo = {
  pid: number | null
  baseUrl: string | null
}

type McpDisconnectResult = {
  pid: number | null
  exited: boolean
}

function requireMcpIsolationProof() {
  return cy
    .task<McpIsolationProof | null>('mcpIsolationProofParams')
    .then((params) => {
      if (!params) {
        throw new Error(
          'MCP isolation proof requires own marker, foreign marker, and barrier dir.'
        )
      }
      return params
    })
}

Given('I connect to an MCP client that connects to Donut MCP service', () => {
  start.mcpAgentActions().connect()
})

Given("I add this worktree's MCP isolation note", () => {
  requireMcpIsolationProof().then((params) => {
    cy.get<string>('@currentLoginUser').then((username) =>
      start
        .testability()
        .injectNoteWithContent(
          params.own,
          `Isolation marker ${params.own}`,
          username,
          'CS concepts',
          'Programming Concepts'
        )
    )
  })
})

Given(
  'I wait until the other worktree has seeded its MCP isolation note',
  () => {
    cy.task('mcpIsolationRendezvousAfterSeed', null, {
      timeout: WORKTREE_RESET_ISOLATION_TASK_TIMEOUT_MS,
    })
  }
)

Then("the MCP client used this checkout's isolated origin", () => {
  expect(
    Boolean(Cypress.expose(WORKTREE_BROWSER_ISOLATION_EXPOSE_KEY)),
    'worktree browser isolation must be active'
  ).to.equal(true)
  cy.task<McpConnectionInfo>('mcpClientConnectionInfo').then((info) => {
    const origin = Cypress.config('baseUrl')
    expect(info.baseUrl, 'MCP client baseUrl').to.equal(origin)
    expect(origin, 'isolated Cypress origin').to.match(
      /^http:\/\/127\.0\.0\.1:\d+$/
    )
    expect(origin, 'must not use the shared primary origin').to.not.include(
      ':5173'
    )
  })
})

When("AI agent searches for this worktree's MCP isolation marker", () => {
  requireMcpIsolationProof().then((params) => {
    start.mcpAgentActions().searchForNote(params.own)
  })
})

Then("the response should contain this worktree's MCP isolation marker", () => {
  requireMcpIsolationProof().then((params) => {
    start.mcpAgentActions().expectResponseContains(params.own)
  })
})

Then(
  "the graph response should contain this worktree's MCP isolation focus note",
  () => {
    requireMcpIsolationProof().then((params) => {
      start.mcpAgentActions().expectGraphContainsFocusNote(params.own)
    })
  }
)

Then(
  "the graph response should not contain the other worktree's MCP isolation marker",
  () => {
    requireMcpIsolationProof().then((params) => {
      start.mcpAgentActions().expectGraphNotContains(params.foreign)
    })
  }
)

When("AI agent searches for the other worktree's MCP isolation marker", () => {
  requireMcpIsolationProof().then((params) => {
    start.mcpAgentActions().searchForNote(params.foreign)
  })
})

When('the MCP client disconnects', () => {
  cy.task<McpDisconnectResult>('disconnectMcpServer').as('mcpDisconnectResult')
})

Then('the spawned MCP server process has exited', () => {
  cy.get<McpDisconnectResult>('@mcpDisconnectResult').then((result) => {
    expect(result.pid, 'spawned MCP server pid').to.be.a('number')
    expect(
      result.exited,
      `MCP server pid ${result.pid} should have exited`
    ).to.equal(true)
  })
})

When('AI agent calls the {string} MCP tool', (apiName: string) => {
  start.mcpAgentActions().callTool(apiName)
})

When(
  'AI agent searches for relevant notes using MCP tool with the term {string}',
  (searchTerm: string) => {
    start.mcpAgentActions().searchForNote(searchTerm)
  }
)

Then('the response should contain {string}', (expectedResponse: string) => {
  start.mcpAgentActions().expectResponseContains(expectedResponse)
})

Then(
  'the search results should include a note with the title {string}',
  (noteTitle: string) => {
    start.mcpAgentActions().expectSearchResultIncludesNoteTitle(noteTitle)
  }
)

Then(
  'the graph response should contain the focus note {string}',
  (noteTitle: string) => {
    start.mcpAgentActions().expectGraphContainsFocusNote(noteTitle)
  }
)

When(
  'AI agent extracts note ID and calls get graph MCP tool with token limit {string}',
  (limit: string) => {
    const tokenLimit = parseInt(limit)
    start.mcpAgentActions().getNoteGraphFromLastSearch(tokenLimit)
  }
)

Then('the graph response should contain {string}', (expectedText: string) => {
  start.mcpAgentActions().expectGraphContains(expectedText)
})

Then(
  'the graph response should not contain {string}',
  (unexpectedText: string) => {
    start.mcpAgentActions().expectGraphNotContains(unexpectedText)
  }
)
