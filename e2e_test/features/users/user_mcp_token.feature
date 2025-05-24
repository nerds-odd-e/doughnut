Feature: User MCP token
  As a user, I want to use MCP token to access Doughnut MCP service.

  Scenario: Generate MCP token
    Given I am logged in as an existing user
    When I generate MCP token
    Then I can use new MCP token
