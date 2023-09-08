@usingMockedOpenAiService
Feature: Question generation by AI
  As a learner, I want to use AI to generate review questions based on my note and its context.
  So that I can remember my note better and potentially get new inspiration.

  Background:
    Given I've logged in as "developer"
    And there are some notes for the current user:
      | topic        | details                                        |
      | Scuba Diving | The most common certification is Rescue Diver. |
    And OpenAI by default returns this question from now:
      | question                                            | correct_choice | incorrect_choice_1 | incorrect_choice_2 |
      | What is the most common scuba diving certification? | Rescue Diver   | Divemaster         | Open Water Diver   |

  Scenario: I should be able to mark the question as bad
    When I ask to generate a question for note "Scuba Diving"
    When I mark the question "What is the most common scuba diving certification?" as bad
    Then I should see the question "What is the most common scuba diving certification?" is marked as bad
    When I attempt to export bad training data
    Then a file with bad training data is produced
