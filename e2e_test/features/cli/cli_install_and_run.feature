@bundleCliE2eInstall
Feature: CLI install and run
  As a user, I want to install the Doughnut CLI, run it, and update it from the server.

  Background:
    Given the backend is serving the CLI and install script
    And I install the CLI from localhost without affecting my system

  Scenario: Install from localhost and verify the CLI version (non-interactive)
    When I run the installed doughnut version command
    Then I should see the installed CLI version in the non-interactive output

  Scenario: Install and run the CLI in interactive mode
    When I run the installed doughnut command in interactive mode
    Then I should see the installed CLI version in past CLI assistant messages
    When I enter the slash command "/exit" in the interactive CLI
    And I should see "/exit" in past user messages

  Scenario: Update to a newer CLI version from the server
    Given the backend serves a newer CLI than the installed version
    When I run the installed doughnut update command with BASE_URL from localhost
    Then I should see that the CLI was updated to the newer version in the non-interactive output
