@usingMockedOpenAiService
Feature: Question generation by AI
  As a learner, I want to use AI to generate questions based on my note and its context.
  So that I can remember my note better and potentially get new inspiration.

  Background:
    Given I am logged in as an existing user

  Scenario Outline: testing myself with generated question for a note
    Given I have a notebook with the head note "Scuba Diving"
    And OpenAI assistant will create these thread ids in sequence: "thread-first-question"
    And OpenAI generates this question for assistant thread "thread-first-question":
      | Question Stem                                       | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is the most common scuba diving certification? | Rescue Diver   | Divemaster         | Open Water Diver   |
    And I test myself for the note "Scuba Diving"
    Then I should be asked "What is the most common scuba diving certification?"
    And the choice "<option>" should be <expectedResult>
    And my question should not be included in the admin's fine-tuning data

    Examples:
      | option       | expectedResult |
      | Rescue Diver | correct        |
      | Divemaster   | incorrect      |
