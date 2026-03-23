@bundleCli
Feature: CLI install and run
  As a user, I want to install the Doughnut CLI, run it, and update it from the server.

  Scenario: Install from localhost and run the CLI in a TTY (interactive)
    Given the backend is serving the CLI and install script
    When I install the CLI from localhost without affecting my system
    And I run the installed doughnut command in interactive mode
    Then I should see "doughnut 0.1.0" in the history output
    And I should see "exit" in the history input

  Scenario: Update from 0.1.0 to 0.2.0
    Given the backend is serving the CLI and install script
    When I install the CLI from localhost without affecting my system
    And I run the installed doughnut version command
    Then I should see "doughnut 0.1.0" in the non-interactive output
    When the backend serves the CLI with version "0.2.0"
    And I run the installed doughnut update command with BASE_URL from localhost
    Then I should see "Updated doughnut from 0.1.0 to 0.2.0" in the non-interactive output
    When I run the installed doughnut version command
    Then I should see "doughnut 0.2.0" in the non-interactive output
