Feature: User Doughnut Access Token
  As a user, I want to use Doughnut Access Token to access Doughnut MCP service.

  Scenario: Generate Doughnut Access Token
    Given I am logged in as "old_learner"
    And I have a notebook "Parent demo"
    When I generate a new Doughnut Access Token with label "Initial Token"
    Then the new Doughnut Access Token should be a valid UUID
    And I can see the token with label "Initial Token" in the list of tokens

  Scenario: Delete a Doughnut Access Token
    Given I am logged in as "old_learner"
    And I have a valid Doughnut Access Token with label "To be deleted"
    And calling token-info with the Doughnut Access Token succeeds
    When I delete the Doughnut Access Token with label "To be deleted"
    Then calling token-info with the Doughnut Access Token is denied
