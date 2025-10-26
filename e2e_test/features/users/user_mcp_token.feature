Feature: User MCP token
  As a user, I want to use MCP token to access Doughnut MCP service.

  Background:
    Given I am logged in as "old_learner"
    And I have a notebook with the head note "Parent Note"

  Scenario: Generate MCP token
    When I generate a new MCP token with label "Initial Token"
    Then the new MCP Token should be a valid UUID
    And I can see the token with label "Initial Token" in the list of tokens

  Scenario: Delete an MCP token
    Given I have a valid MCP token with label "To be deleted"
    And I can create a note as a child of "Parent Note" using the MCP token
    When I delete the MCP token with label "To be deleted"
    Then I cannot create a note as a child of "Parent Note" using the MCP token

  Scenario: Display last used time for MCP token
    Given I have a valid MCP token with label "Usage Token"
    And the MCP token "Usage Token" has not been used

  Scenario: User MCP token expiration
    Given I have a valid MCP token with label "my token"
    When it is 90 days later
    Then the MCP token "my token" should be marked as expired
