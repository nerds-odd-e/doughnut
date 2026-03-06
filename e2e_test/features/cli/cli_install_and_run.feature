Feature: CLI install and run
  As a user, I want to install the Doughnut CLI and run it.

  @bundleAndCopyCliToBackendResources
  Scenario: Install from localhost and run interactive CLI
    Given the backend is serving the CLI and install script
    When I install the CLI from localhost without affecting my system
    And I run the installed doughnut command
    Then I should see "doughnut 0.1.0"
    And I should see "exit"

  Scenario: Interactive CLI responds "Not supported" to input
    When I run the doughnut command with input "hello"
    Then I should see "Not supported"

  Scenario: Interactive CLI exits on exit command
    When I run the doughnut command with input "exit"
    Then I should see "doughnut 0.1.0"

  Scenario: -c option processes one input and exits
    When I run the doughnut command with -c "hello"
    Then I should see "doughnut 0.1.0"
    And I should see "Not supported"

  Scenario: Show version
    When I run the doughnut version command
    Then I should see "doughnut 0.1.0"

  @bundleAndCopyCliToBackendResources
  Scenario: Update when already latest (0.1.0)
    Given the backend is serving the CLI and install script
    When I install the CLI from localhost without affecting my system
    When I run the installed doughnut update command with BASE_URL from localhost
    Then I should see "doughnut 0.1.0 is already the latest version"

  @bundleAndCopyCliToBackendResources
  Scenario: Update from 0.1.0 to 0.2.0
    Given the backend is serving the CLI and install script
    And the CLI is built with version "0.1.0"
    When I install the CLI from localhost without affecting my system
    And I run the installed doughnut version command
    Then I should see "doughnut 0.1.0"
    When the backend serves the CLI with version "0.2.0"
    And I run the installed doughnut update command with BASE_URL from localhost
    Then I should see "Updated doughnut from 0.1.0 to 0.2.0"
    When I run the installed doughnut version command
    Then I should see "doughnut 0.2.0"
