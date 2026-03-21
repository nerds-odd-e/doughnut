Feature: CLI interactive mode
  As a user, I want the TTY interactive CLI to record input and render the UI correctly.

  @withCliConfig
  @interactiveCLI
  Scenario: TTY interactive responds "Not supported" to a plain line
    When I enter "hello" in the interactive CLI
    Then I should see "Not supported" in the history output

  @withCliConfig
  @interactiveCLI
  Scenario: After /clear, Enter on empty input does not show duplicate top border
    When I enter the slash command "/clear" in the interactive CLI
    And I press Enter in the interactive CLI
    And I press Enter in the interactive CLI
    Then the input box UI should be normal
