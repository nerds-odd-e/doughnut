Feature: Handling MCP Token

  Background:
    Given I am logged in as an existing user

  @ignore
  Scenario: Generate MCP Token
    When I click "MCP Token" in side menu
    When I click button "generate"
    Then I should see geneted MCP Token

  @ignore
  Scenario: Delete MCP Token
    When I click "MCP Token" in side menu
    When I click button "delete"
    Then I should see empty MCP Token