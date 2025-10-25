import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'
import start from '../start'

Given(
  'I connect to an MCP client that connects to Doughnut MCP service',
  () => {
    start.mcpAgentActions().connect()
  }
)

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

When(
  'AI agent adds note via MCP tool to add note {string} under {string}',
  (noteTitle: string, parentTitle: string) => {
    start.mcpAgentActions().addNote(parentTitle, noteTitle)
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
