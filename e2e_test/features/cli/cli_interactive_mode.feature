Feature: CLI interactive mode
  As a user, I want the TTY interactive CLI to record input and render the UI correctly.

  @withCliConfig
  @interactiveCLI
  Scenario: TTY interactive responds "Not supported" to a plain line
    When I enter "hello" in the interactive CLI
    Then I should see "Not supported" in the history output
    And I should see "hello" in the history input

  @withCliConfig
  @interactiveCLI
  Scenario: After /help, consecutive Enter on empty input keeps a normal input box
    When I enter the slash command "/help" in the interactive CLI
    And I press Enter in the interactive CLI
    And I press Enter in the interactive CLI
    And I press Enter in the interactive CLI
    Then the input box UI should be normal

  @withCliConfig
  @interactiveCLI
  Scenario: /help lists subcommands and interactive commands
    When I enter the slash command "/help" in the interactive CLI
    Then I should see "/add gmail" in the history output
    And I should see "/last email" in the history output
    And I should see "exit" in the history output
    And I should see "update" in the history output
    And I should see "version" in the history output

  @withCliConfig
  @interactiveCLI
  Scenario: exit ends the session after Bye
    When I enter "exit" in the interactive CLI
    Then I should see "Bye." in the history output
