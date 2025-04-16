Feature: Note creation

  @ignore
  Scenario: Create new note under the default notebook
    Given MCP server is running
    When Call instruction API by MCP Client
    Then Return Doughnut instruction

