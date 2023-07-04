@usingMockedOpenAiService
Feature: Question generation by AI
  As a learner, I want to use AI to generate review questions based on my note and its context.
  So that I can remember my note better and potentially get new inspiration.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title        | description                                    |
      | Scuba Diving | The most common certification is Rescue Diver. |
    And OpenAI by default returns this question from now:
      | question                                            | correct_choice | incorrect_choice_1 | incorrect_choice_2 |
      | What is the most common scuba diving certification? | Rescue Diver   | Divemaster         | Open Water Diver   |

  Scenario Outline: testing myself with generated question for a note
    When I ask to generate a question for note "Scuba Diving"
    Then I should be asked "What is the most common scuba diving certification?"
    And the option "<option>" should be <expectedResult>
    Examples:
      | option       | expectedResult |
      | Rescue Diver | correct        |
      | Divemaster   | wrong          |

  @ignore
  Scenario: I should be able to regenerate the question when the question and choices do not make sense
    Given I have a note with title "Mike likes elephants and tigers, hates dogs."
    And OpenAI by default returns this question from now:
      | question                | correct_choice | incorrect_choice_1 | incorrect_choice_2 |
      | How often scuba diving? | daily          | weekly             | never              |
    And openAI thinks its question doesn't make sense
    When I ask it to regenerete the question while testing myself
    Then I should be asked with a new question "what does Mike like" with choices "Elephants", "Dogs", "Tigers"

  @ignore
  Scenario: I should be able to affect the question using note instruction
    When Note "Scuba Diving" has instruction "Location is Singapore, amongst the ships off the East Coast Park."
    Then Question generated from the note "Scuba Diving" is "Is the water clean for Scuba Diving in Singapore?"

  @ignore
  Scenario:  I should be able to regenerate the question when the question and choices do not make sense
    Given I ask to generate a question for note "Scuba Diving"
    And OpenAI by default returns this question from now:
      | question                | correct_choice | incorrect_choice_1 | incorrect_choice_2 |
      | How often scuba diving? | daily          | weekly             | never              |
    When I regenerate the question
    Then I should be asked "How often scuba diving?"
  @ignore
  Scenario: I should be able to regenerate the question when I think the question and choices do not make sense
    Given I have a note with title "Mike likes elephants and tigers, hates dogs."
    And openAI provide question as:
      | question            | correct_choice | incorrect_choice_1 | incorrect_choice_2 |
      | what does Mike hate | dogs           | elephants          | tigers             |
    And openAI insist its question makes sense
    When I ask it to regenerete the question while testing myself
    Then I should be asked with the same question
