@ignore
@withCliConfig
@interactiveCLI
Feature: CLI access token
  As a learner, I want to save a Donut Access Token in the interactive CLI so authenticated commands work.

  Background:
    Given I am logged in as "old_learner"

  @skipOptimizationDueToKnownNecessarySlowness
  Scenario: Saved access token allows recall status
    Given I have a valid Donut Access Token with label "E2E CLI Token"
    And I save the Donut Access Token in the interactive CLI
    When I enter the slash command "/recall-status" in the interactive CLI
    Then I should see "0 notes to recall today" in past CLI assistant messages

  Scenario: Invalid access token is rejected
    When I enter the slash command "/set-access-token invalid-token-xxx" in the interactive CLI
    Then I should see "Access token is invalid or expired" in past CLI assistant messages
