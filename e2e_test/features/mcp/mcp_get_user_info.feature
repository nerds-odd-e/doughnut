Feature: MCP (Model Context Protocol) get user info

  Background:
    Given I am logged in as an existing user
    And I have a valid MCP token
    #And a MCP client connected to MCP server that connnected to Doughnut MCP service


   @ignore
   Scenario Outline: Retrieve basic user information

    When the client requests user information via MCP service
    Then the response should contain "<userName>"
    Examples:
      | userName                  |
      | Error Message             |
      | userName               |
      | Null or Empty userName |