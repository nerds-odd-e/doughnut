@withCliConfig
@interactiveCLI
Feature: CLI interactive mode
  As a user, I want the TTY interactive CLI to record input and render the UI correctly.

  Scenario: TTY interactive responds "Not supported" to a plain line
    When I enter "hello" in the interactive CLI
    Then I should see "Not supported" in past CLI assistant messages
    And I should see "hello" in past user messages

  Scenario: /help lists subcommands and interactive commands
    When I enter the slash command "/help" in the interactive CLI
    Then I should see "/help" in past CLI assistant messages
    And I should see "/add gmail" in past CLI assistant messages
    And I should see "/last email" in past CLI assistant messages
    And I should see "/exit" in past CLI assistant messages
    And I should see "update" in past CLI assistant messages
    And I should see "version" in past CLI assistant messages

  Scenario: exit ends the session after Bye
    When I enter the slash command "/exit" in the interactive CLI
    Then I should see "Bye." in past CLI assistant messages
