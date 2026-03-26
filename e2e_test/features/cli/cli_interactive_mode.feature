Feature: CLI interactive mode
  As a user, I want the TTY interactive CLI to record input and render the UI correctly.

  @withCliConfig
  @interactiveCLI
  Scenario: TTY interactive responds "Not supported" to a plain line
    When I enter "hello" in the interactive CLI
    Then I should see "Not supported" in past CLI assistant messages
    And I should see "hello" in past user messages

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
    Then I should see "/add gmail" in past CLI assistant messages
    And I should see "/last email" in past CLI assistant messages
    And I should see "exit" in past CLI assistant messages
    And I should see "update" in past CLI assistant messages
    And I should see "version" in past CLI assistant messages

  @withCliConfig
  @interactiveCLI
  Scenario: exit ends the session after Bye
    When I enter "exit" in the interactive CLI
    Then I should see "Bye." in past CLI assistant messages
