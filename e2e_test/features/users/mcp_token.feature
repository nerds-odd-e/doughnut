Feature: Handling MCP Token

  Background:
    Given I am logged in as an existing user

  Scenario: Generate MCP Token
    When I generate MCP Token
    Then I should see generated Token

  @ignore
  Scenario: Generate MCP Token & Reload Page
    When I generate MCP Token and I reload page
    Then I should see same Token
    
  Scenario: Delete MCP Token
    Given I have MCP Token
    When I delete MCP Token
    Then I should see empty MCP Token

  @ignore
  Scenario: Authenticate user with a valid token
    Given I have MCP Token
    When I query MCP server with MCP token
    Then the request should be successful