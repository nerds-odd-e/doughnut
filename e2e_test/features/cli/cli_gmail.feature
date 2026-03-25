@usingMockedGoogleService
@cliGmailBundledSecrets
Feature: CLI Gmail integration
  As a user, I want to connect my Gmail account and read the last email subject.

  @withCliGmailOAuthAddConfig
  @interactiveCLIGmailOAuth
  Scenario: add gmail adds account when OAuth callback is simulated
    Given the Google API mock returns tokens and profile for "e2e@gmail.com"
    When I enter the slash command "/add gmail" in the interactive CLI
    Then I should see "Added account e2e@gmail.com" in the history output

  @withCliGmailMockAccountConfig
  @interactiveCLIGmail
  Scenario: last email shows subject when account is configured
    Given the Google API mock returns messages and message "msg-1" with subject "Welcome to Doughnut"
    When I enter the slash command "/last email" in the interactive CLI
    Then I should see "Welcome to Doughnut" in the history output
