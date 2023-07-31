@ignore
@usingMockedOpenAiService
Feature: Question generation by AI
  As a learner, I want to use AI to generate review questions based on my note and its context.
  So that I can remember my note better and potentially get new inspiration.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user:
      | title        | description                                    |
      | Scuba Diving | The most common certification is Rescue Diver. |
    And OpenAI by default returns this question from now:
      | question                                            | correct_choice | incorrect_choice_1 | incorrect_choice_2 |
      | What is the most common scuba diving certification? | Rescue Diver   | Divemaster         | Open Water Diver   |

  Scenario Outline: testing myself with generated question for a note
    When I ask to generate a question for note "Scuba Diving"
    Then I should be asked "What is the most common scuba diving certification?"
    And the choice "<option>" should be <expectedResult>
    Examples:
      | option       | expectedResult |
      | Rescue Diver | correct        |
      | Divemaster   | incorrect      |

  Scenario: I should be able to regenerate the question when the question and choices do not make sense relating to the note
    When I ask to generate a question for note "Scuba Diving"
    And OpenAI by default returns this question from now:
      | question              | correct_choice | incorrect_choice_1 | incorrect_choice_2 |
      | What is scuba diving? | Rescue Diver   | Divemaster         | Open Water Diver   |
    Then I complain the question doesn't make sense
    And I should see the question "What is the most common scuba diving certification?" is disabled
    And I should be asked "What is scuba diving?"
