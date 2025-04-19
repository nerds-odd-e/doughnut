Feature: get instruction
  Return Doughnut instruction

  Scenario: get instruction
    Given I connect to an MCP client that connects to Doughnut MCP service
    When Call instruction API by MCP Client
    Then Return Doughnut instruction
