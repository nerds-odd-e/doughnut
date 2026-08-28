@ignore
Feature: CLI Gmail
  As a learner, I want to connect Gmail in the interactive CLI and read the last email subject.

  @usingMockedGoogleService
  @withCliGmailOAuthAddConfig
  @interactiveCLIGmail
  Scenario: Add Gmail account via OAuth
    Given the Google API mock returns tokens and profile for "e2e@gmail.com"
    And the interactive CLI has Google OAuth callback simulation enabled
    When I enter the slash command "/add gmail" in the interactive CLI
    Then I should see "Added account e2e@gmail.com" in past CLI assistant messages

  @usingMockedGoogleService
  @withCliGmailMockAccountConfig
  @interactiveCLIGmail
  Scenario: Last email shows the subject for a configured account
    Given the Google API mock returns messages and message "msg-1" with subject "Welcome to Donut"
    When I enter the slash command "/last email" in the interactive CLI
    Then I should see "Welcome to Donut" in past CLI assistant messages
