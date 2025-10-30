Feature: User MCP token
  As a user, I want to use MCP token to access Doughnut MCP service.

  Background:
    Given I am logged in as "old_learner"
    And I have a notebook with the head note "Parent Note"

  Scenario: Generate MCP token
    When I generate a new MCP token with label "Initial Token"
    Then the new MCP Token should be a valid UUID
    And I can see the token with label "Initial Token" in the list of tokens
    And The last used token timestamp should show "N/A"

  Scenario Outline: Token expiration
    Given I have an MCP token with expiration date "<expires_at>"
    And the current date is "<current_date>"
    Then the token is marked as "<status>"

  Examples:
    | expires_at | current_date | status  |
    | 2025-01-06 | 2025-01-05   | Valid   |
    #| 2025-01-06 | 2025-01-07   | Invalid |

  Scenario: Delete an MCP token
    Given I have a valid MCP token with label "To be deleted"
    And I can create a note as a child of "Parent Note" using the MCP token
    When I delete the MCP token with label "To be deleted"
    Then I cannot create a note as a child of "Parent Note" using the MCP token

  @focus
  Scenario: MCP token last used timestamp updates after use
    Given I have a valid MCP token with label "Tracking Test Token"
    When I can create a note as a child of "Parent Note" using the MCP token
    Then the last used token timestamp should show "2025-10-29 10:00:00"