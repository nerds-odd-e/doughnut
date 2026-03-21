@bundleAndCopyCliToBackendResources
Feature: CLI non-interactive mode
  As a user, I want the CLI to behave predictably when run with piped input, -c, or subcommands.

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
