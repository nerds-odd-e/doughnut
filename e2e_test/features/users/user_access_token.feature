Feature: User Doughnut Access Token
  As a user, I want to use a Doughnut Access Token to access the Doughnut MCP service.

  Background:
    Given I am logged in as "old_learner"

  Scenario: Generate Doughnut Access Token
    When I generate a new Doughnut Access Token with label "Initial Token"
    Then the new Doughnut Access Token should be a valid UUID
    And the token with label "Initial Token" is listed

  Scenario: Delete a Doughnut Access Token
    Given I have a valid Doughnut Access Token with label "To be deleted"
    And calling token-info with the Doughnut Access Token succeeds
    When I delete the Doughnut Access Token with label "To be deleted"
    Then calling token-info with the Doughnut Access Token is denied
