Feature: Note edition

  Background:
    Given MCP server is running
    And user exists

  @ignore
  Scenario Outline: Update a note title
    Given I have a note with title "Dog" and detail "Dog is cute"
    And I have a valid authentication token on the MCP client
    When I ask the MCP client to update the note title to "<inputTitle>"
    Then MCP server returns updated note titled "<resultTitle>"

    Examples:
      | inputTitle | resultTitle |
      | Cat        | Cat         |
      |            | Dog         |

  @ignore
  Scenario Outline: Update a note details
    Given I have a note with title "Dog" and detail "Dog is cute"
    And I have a valid authentication token on the MCP client
    When I ask the MCP client to update the note details to "<inputDetails>"
    Then MCP server returns updated note with details "<resultDetails>"

    Examples:
      | inputDetails | resultDetails |
      | Cat is cute  | Cat is cute   |
      |              | Dog is cute   |
