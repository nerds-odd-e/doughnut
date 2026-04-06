@withCliConfig
@interactiveCLI
Feature: CLI access token management

  Background:
    Given I am logged in as "old_learner"

  Scenario: Add access token
    Given I have a valid Doughnut Access Token with label "E2E CLI Token"
    And I add the saved access token in the interactive CLI using add-access-token
    When I enter the slash command "/recall-status" in the interactive CLI
    Then I should see "0 notes to recall today" in past CLI assistant messages

  Scenario: Add invalid access token
    When I enter the slash command "/add-access-token invalid-token-xxx" in the interactive CLI
    Then I should see "Access token is invalid or expired" in past CLI assistant messages
