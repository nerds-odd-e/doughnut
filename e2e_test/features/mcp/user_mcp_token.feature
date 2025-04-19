Feature: User MCP Token
  As a learner, I want to have a token to access Doughnut via MCP protocol.

  Background:
    Given I am logged in as an existing user

  Scenario: Delete MCP Token
    Given I have a MCP Token
    When I delete my MCP Token
    Then I should see empty MCP Token
