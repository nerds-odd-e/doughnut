Feature: CLI install and run
  As a user, I want to install the Doughnut CLI and run it.

  @bundleAndCopyCliToBackendResources
  Scenario: Install from localhost and run interactive CLI
    Given the backend is serving the CLI and install script
    When I install the CLI from localhost without affecting my system
    And I run the installed doughnut command
    Then I should see "doughnut 0.1.0" in the history output
    And I should see "exit" in the history input

  Scenario: Interactive CLI responds "Not supported" to input
    When I run the doughnut command with input "hello"
    Then I should see "Not supported" in the history output

  Scenario: Interactive CLI exits on exit command
    When I run the doughnut command with input "exit"
    Then I should see "doughnut 0.1.0" in the history output

  @withCliConfig
  Scenario: -c option processes one input and exits
    When I run the doughnut command with -c "hello"
    Then I should see "doughnut 0.1.0" in the history output
    And I should see "Not supported" in the history output

  Scenario: Show version
    When I run the doughnut version command
    Then I should see "doughnut 0.1.0" in the history output

  @withCliConfig
  Scenario: Interactive /help command lists all commands
    When I run the doughnut command with -c "/help"
    Then I should see "/add gmail" in the history output
    And I should see "/last email" in the history output
    And I should see "exit" in the history output
    And I should see "update" in the history output
    And I should see "version" in the history output

  @bundleAndCopyCliToBackendResources
  Scenario: Update from 0.1.0 to 0.2.0
    Given the backend is serving the CLI and install script
    When I install the CLI from localhost without affecting my system
    And I run the installed doughnut version command
    Then I should see "doughnut 0.1.0" in the history output
    When the backend serves the CLI with version "0.2.0"
    And I run the installed doughnut update command with BASE_URL from localhost
    Then I should see "Updated doughnut from 0.1.0 to 0.2.0" in the history output
    When I run the installed doughnut version command
    Then I should see "doughnut 0.2.0" in the history output
