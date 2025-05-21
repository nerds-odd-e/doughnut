Feature: MCP (Model Context Protocol) get user info

  Background:
    Given I am logged in as an existing user
    And I have a valid MCP token
    And I connect to an MCP client that connects to Doughnut MCP service

  Scenario Outline: Retrieve basic user information
    When the client requests user information via MCP service
    Then the response should contain "<userName>"
    Examples:
      | userName                  |
      | old_learner               |

