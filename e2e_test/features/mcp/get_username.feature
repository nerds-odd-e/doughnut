Feature: get username
  Return username

  Background:
    Given I am logged in as an existing user
  
  Scenario: get username
    Given MCP server is running
    And I generate MCP Token
    When Call get username tool by MCP Client
    Then Return username