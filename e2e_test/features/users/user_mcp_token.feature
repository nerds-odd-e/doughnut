Feature: User MCP token
  As a user, I want to use MCP token to access Doughnut MCP service.

  Scenario: Generate MCP token
    Given I am logged in as an existing user
    When I generate a new MCP token
    Then the new MCP Token should be a valid UUID
