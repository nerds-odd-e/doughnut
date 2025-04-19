Feature: get username
  Return username

  Background:
    Given I am logged in as an existing user

  Scenario: get username
    Given I connect to an MCP client that connects to Doughnut MCP service
    And I generate MCP Token
    When Call get username tool by MCP Client
    Then Return username
