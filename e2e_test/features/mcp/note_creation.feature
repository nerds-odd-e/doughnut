Feature: Note creation
  Background: 
    Given MCP server is running

  @ignore
  Scenario: Create new note under the default notebook
    Given User has the default notebook
    When Call instruction API by MCP Client
    Then Return Doughnut instruction
