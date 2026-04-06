@withCliConfig
@interactiveCLI
Feature: CLI access token management

  Background:
    Given I am logged in as "old_learner"

  Scenario: Add access token and list it
    Given I have a valid Doughnut Access Token with label "E2E CLI Token"
    When I add the saved access token in the interactive CLI using add-access-token
    Then I enter the slash command "/list-access-token" in the interactive CLI
    And I should see "E2E CLI Token" in the Current guidance

  Scenario: Add invalid access token
    When I enter the slash command "/add-access-token invalid-token-xxx" in the interactive CLI
    Then I should see "Access token is invalid or expired" in past CLI assistant messages
