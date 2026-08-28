Feature: User Donut Access Token
  As a user, I want to use a Donut Access Token to access the Donut MCP service.

  Background:
    Given I am logged in as "old_learner"

  Scenario: Generate Donut Access Token
    When I generate a new Donut Access Token with label "Initial Token"
    Then the new Donut Access Token should be a valid UUID
    And the token with label "Initial Token" is listed

  Scenario: Delete a Donut Access Token
    Given I have a valid Donut Access Token with label "To be deleted"
    And calling token-info with the Donut Access Token succeeds
    When I delete the Donut Access Token with label "To be deleted"
    Then calling token-info with the Donut Access Token is denied
