Feature: User Doughnut Access Token
  As a user, I want to use Doughnut Access Token to access Doughnut MCP service.

  Background:
    Given I am logged in as "old_learner"
    And I have a notebook with the head note "Parent Note"

  Scenario: Generate Doughnut Access Token
    When I generate a new Doughnut Access Token with label "Initial Token"
    Then the new Doughnut Access Token should be a valid UUID
    And I can see the token with label "Initial Token" in the list of tokens

  Scenario: Delete a Doughnut Access Token
    Given I have a valid Doughnut Access Token with label "To be deleted"
    And I can create a note as a child of "Parent Note" using the Doughnut Access Token
    When I delete the Doughnut Access Token with label "To be deleted"
    Then I cannot create a note as a child of "Parent Note" using the Doughnut Access Token
