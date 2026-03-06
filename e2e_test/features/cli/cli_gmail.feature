@cli
@usingMockedGoogleService
Feature: CLI Gmail integration
  As a user, I want to connect my Gmail account and read the last email subject.

  Scenario: add gmail adds account when OAuth callback is simulated
    Given the backend is serving the CLI and install script
    And the Google API mock returns tokens and profile for "e2e@gmail.com"
    When I install the CLI from localhost without affecting my system
    And I run the CLI add gmail command with simulated OAuth callback
    Then I should see "Added account e2e@gmail.com"

  Scenario: last email shows subject when account is configured
    Given the backend is serving the CLI and install script
    And the Google API mock returns messages and message "msg-1" with subject "Welcome to Doughnut"
    When I install the CLI from localhost without affecting my system
    And I run the CLI last email command with pre-configured account
    Then I should see "Welcome to Doughnut"
