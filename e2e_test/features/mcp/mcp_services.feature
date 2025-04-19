Feature: MCP (Model Context Protocol) Services
  As a note taker, I want my AI clients like Cursor to use the MCP services from
  Doughnut, so that AI can automatically update my notes and fetch informat from my
  notes.

  Background:
    Given I am logged in as an existing user
    And I generate MCP Token
    And I connect to an MCP client that connects to Doughnut MCP service

  Scenario Outline: MCP API calls
    When Call <api_name> tool by MCP Client
    Then Return <return_value>

    Examples:
      | api_name     | return_value          |
      | instruction  | Doughnut instruction  |
      | get username | username              |
