@ignore
@withCliConfig
@interactiveCLI
Feature: CLI interactive session
  As a learner, I want the interactive CLI to accept input, list commands, and end the session cleanly.

  Scenario: Unsupported plain line is rejected
    When I enter "hello" in the interactive CLI
    Then I should see "Not supported" in past CLI assistant messages
    And I should see "hello" in past user messages

  Scenario: Help lists interactive and non-interactive commands
    When I enter the slash command "/help" in the interactive CLI
    Then I should see "/help" in past CLI assistant messages
    And I should see "/add gmail" in past CLI assistant messages
    And I should see "/last email" in past CLI assistant messages
    And I should see "/exit" in past CLI assistant messages
    And I should see "update" in past CLI assistant messages
    And I should see "version" in past CLI assistant messages

  Scenario: Exit ends the session
    When I enter the slash command "/exit" in the interactive CLI
    Then I should see "Bye." in past CLI assistant messages
