Feature: MCP (Model Context Protocol) get user info

  Background:
    Given I am logged in as an existing user
    And I connect to an MCP client that connects to Doughnut MCP service

  @ignore
   Scenario: Retrieve basic user information
    Given the user is authenticated
    When the client requests user information via MCP service
    Then the MCP service returns the user's info (username, etc)
    And the user information is correctly formatted
