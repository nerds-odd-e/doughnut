Feature: Generate MCP token

  Scenario: Generate MCP token
    Given I am logged in as an existing user
    When I generate MCP token
    Then I can use new MCP token
    