Feature: CLI interactive mode
  As a user, I want the TTY interactive CLI to record input and render the UI correctly.

  @bundleAndCopyCliToBackendResources
  Scenario: Installed CLI records exit in history input when quitting
    Given the backend is serving the CLI and install script
    When I install the CLI from localhost without affecting my system
    And I run the installed doughnut command in interactive mode
    Then I should see "exit" in the history input

  @withCliConfig
  @interactiveCLI
  Scenario: After /clear, Enter on empty input does not show duplicate top border
    When I input "/clear" in the interactive CLI
    And I press Enter in the interactive CLI
    And I press Enter in the interactive CLI
    Then the input box UI should be normal
