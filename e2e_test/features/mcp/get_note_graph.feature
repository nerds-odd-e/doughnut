Feature: MCP (Model Context Protocol) get note graph

  Background:
    Given I have a note id
    And I connect to an MCP client that connects to Doughnut MCP service

  @ignore
  Scenario: Retrieve graph with note id
  Given the note id
  When the client requests read note with graph via MCP service
  Then the MCP service returns the json of the graph from note
  And the json is correctly formatted
