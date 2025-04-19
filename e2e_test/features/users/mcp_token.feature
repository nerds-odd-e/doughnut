Feature: Handling MCP Token

  Background:
    Given I am logged in as an existing user

  Scenario: Generate MCP Token
    When I generate a MCP Token
    Then I should see generated Token

  Scenario: Delete MCP Token
    Given I have a MCP Token
    When I delete MCP Token
    Then I should see empty MCP Token

  Scenario: Delete MCP Token & Reload Page
    Given I have a MCP Token
    When I delete MCP Token and I reload page
    Then I should see empty MCP Token

  @ignore
  Scenario: Authenticate user with a valid token
    Given I have a MCP Token
    When I query MCP server with MCP token
    Then the request should be successful
