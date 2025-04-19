Feature: Note edition

  Background:
    Given I connect to an MCP client that connects to Doughnut MCP service
    And user exists

  @ignore
  Scenario Outline: Update a note
    Given I have a note with title "Dog" and detail "Dog is cute"
    And I have a valid authentication token on the MCP client
    When I ask the MCP client to update the note title to "<inputTitle>" and details to "<inputDetails>"
    Then MCP server returns updated note titled "<resultTitle>" with details "<resultDetails>"

    Examples:
      | inputTitle | inputDetails | resultTitle | resultDetails |
      | Cat        | Cat is cute  | Cat         | Cat is cute   |
      | Cat        |              | Cat         |               |
      |            | Cat is cute  | Dog         | Dog is cute   |
      |            |              | Dog         | Dog is cute   |

  @ignore
  Scenario: Update note with invalid authentication
    Given I have a note with title "Dog" and detail "Dog is cute"
    And I have an invalid authentication token on the MCP client
    When I ask the MCP client to update the note title to "Cat"
    Then MCP server returns an error
