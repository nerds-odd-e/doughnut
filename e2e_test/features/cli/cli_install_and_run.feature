@bundleCliE2eInstall
Feature: CLI install and run
  As a learner, I want to install the Doughnut CLI from the server, check its version, use it interactively, and update it.

  Background:
    Given the backend is serving the CLI and install script
    And I install the CLI from localhost without affecting my system

  Scenario: Installed CLI reports its version
    When I run the installed doughnut version command
    Then I should see the installed CLI version in the non-interactive output

  Scenario: Installed CLI opens an interactive session
    When I run the installed doughnut command in interactive mode
    Then I should see the installed CLI version in past CLI assistant messages
    When I enter the slash command "/exit" in the interactive CLI
    Then I should see "/exit" in past user messages

  Scenario: Installed CLI updates to a newer version from the server
    Given the backend serves a newer CLI than the installed version
    When I run the installed doughnut update command with BASE_URL from localhost
    Then I should see that the CLI was updated to the newer version in the non-interactive output
