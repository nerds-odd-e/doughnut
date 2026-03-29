@withCliConfig
@interactiveCLI
Feature: CLI Gmail integration
  As a user, I want to connect my Gmail account and read the last email subject.

  @ignore
  @usingMockedGoogleService
  Scenario: add gmail adds account when OAuth callback is simulated
    Given the Google API mock returns tokens and profile for "e2e@gmail.com"
    When I enter the slash command "/add gmail" in the interactive CLI
    Then I should see "Added account e2e@gmail.com" in past CLI assistant messages

  @ignore
  @usingMockedGoogleService
  Scenario: last email shows subject when account is configured
    Given the Google API mock returns tokens and profile for "e2e@gmail.com"
    When I enter the slash command "/last email" in the interactive CLI
    Then I should see "Welcome to Doughnut" in past CLI assistant messages
