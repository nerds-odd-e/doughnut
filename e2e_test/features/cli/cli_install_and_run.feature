Feature: CLI install and run
  As a user, I want to install the Doughnut CLI and run it.

  @bundleAndCopyCliToBackendResources
  Scenario: Install from localhost and run the CLI in a TTY (interactive)
    Given the backend is serving the CLI and install script
    When I install the CLI from localhost without affecting my system
    And I run the installed doughnut command in interactive mode
    Then I should see "doughnut 0.1.0" in the history output
    And I should see "exit" in the history input

  Scenario: Piped stdin responds "Not supported" to a line of input
    When I run the doughnut command with input "hello"
    Then I should see "Not supported" in the non-interactive output

  Scenario: Piped stdin exits when the line is exit
    When I run the doughnut command with input "exit"
    Then I should see "doughnut 0.1.0" in the non-interactive output

  @withCliConfig
  Scenario: -c runs one command and exits (non-interactive)
    When I run the doughnut command with -c "hello"
    Then I should see "doughnut 0.1.0" in the non-interactive output
    And I should see "Not supported" in the non-interactive output

  Scenario: version subcommand prints the CLI version
    When I run the doughnut version command
    Then I should see "doughnut 0.1.0" in the non-interactive output

  @withCliConfig
  Scenario: -c "/help" prints help (non-interactive, not the TTY UI)
    When I run the doughnut command with -c "/help"
    Then I should see "/add gmail" in the non-interactive output
    And I should see "/last email" in the non-interactive output
    And I should see "exit" in the non-interactive output
    And I should see "update" in the non-interactive output
    And I should see "version" in the non-interactive output

  @bundleAndCopyCliToBackendResources
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
