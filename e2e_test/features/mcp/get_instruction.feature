Feature: get instruction
  Return Doughnut instruction

  Scenario: get instruction
    Given MCP server is running
    When Call instruction API by MCP Client
    Then Return Doughnut instruction
