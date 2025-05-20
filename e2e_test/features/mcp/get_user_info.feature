Feature: MCP (Model Context Protocol) get user info

  Background:
    Given I connect to an MCP client that connects to Doughnut MCP service

  @ignore
   Scenario Outline: Retrieve basic user information
    Given header contains "<mcpToken>"
    When the client requests user information via MCP service
    Then The "<userName>" is displayed
    Examples:
      | mcpToken      | userName                  |
      | none          | Error Message             |
      | exist         | userName               |
      | non_exist     | Null or Empty userName |