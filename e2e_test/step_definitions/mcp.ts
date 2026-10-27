import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'
import {
  WORKTREE_BROWSER_ISOLATION_EXPOSE_KEY,
  WORKTREE_RESET_ISOLATION_TASK_TIMEOUT_MS,
} from '../../scripts/worktree-isolation-constants.mjs'
import { MCP_HTTP_PATH } from '../support/mcp_note_context_tools'
import start from '../start'

type McpIsolationProof = {
  own: string
  foreign: string
  barrierDir: string
}

type McpConnectionInfo = {
  mcpUrl: string | null
  forwardOrigin: string | null
  tools: string[] | null
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

Then("the MCP client used this checkout's isolated HTTPS MCP endpoint", () => {
  expect(
    Boolean(Cypress.expose(WORKTREE_BROWSER_ISOLATION_EXPOSE_KEY)),
    'worktree browser isolation must be active'
  ).to.equal(true)
  cy.task<McpConnectionInfo>('mcpClientConnectionInfo').then((info) => {
    const origin = Cypress.config('baseUrl')
    expect(info.forwardOrigin, 'MCP TLS forward origin').to.equal(origin)
    expect(info.mcpUrl, 'MCP HTTPS URL').to.match(/^https:\/\/127\.0\.0\.1:\d+/)
    expect(new URL(info.mcpUrl!).pathname, 'MCP HTTPS path').to.equal(
      MCP_HTTP_PATH
    )
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
    start.mcpAgentActions().listNoteCandidates(params.own)
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
    start.mcpAgentActions().listNoteCandidates(params.foreign)
  })
})

When('the MCP client disconnects', () => {
  cy.task('disconnectMcp')
})

Then('the MCP client has discovered the note context tools', () => {
  start.mcpAgentActions().expectDiscoveredNoteContextTools()
})

Then(
  'equal-title note candidates are disambiguated by notebook and folder',
  () => {
    start.mcpAgentActions().expectEqualTitleCandidatesDisambiguated()
  }
)

When('AI agent calls the {string} MCP tool', (apiName: string) => {
  start.mcpAgentActions().callTool(apiName)
})

When(
  'AI agent searches for relevant notes using MCP tool with the term {string}',
  (searchTerm: string) => {
    start.mcpAgentActions().listNoteCandidates(searchTerm)
  }
)

Then('the response should contain {string}', (expectedResponse: string) => {
  start.mcpAgentActions().expectResponseContains(expectedResponse)
})

Then('the note candidates should be {string}', (titles: string) => {
  start.mcpAgentActions().expectCandidateTitles(titles.split(', '))
})

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

When('AI agent reads the first candidate note', () => {
  start.mcpAgentActions().readNoteFromLastSearch()
})

Then(
  'the read note content should contain {string} with an accepted revision to cite',
  (content: string) => {
    start
      .mcpAgentActions()
      .expectReadContentContains(content)
      .rememberReadRevision()
  }
)

Then('the read note should cite a different accepted revision', () => {
  start.mcpAgentActions().expectReadRevisionDiffersFromCited()
})
