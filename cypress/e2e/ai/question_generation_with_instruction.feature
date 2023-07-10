@usingMockedOpenAiService
Feature: Question generation by AI
  As a learner, I want to use AI to generate review questions based on my note and its context.
  So that I can remember my note better and potentially get new inspiration.

  Background:
    Given I've logged in as an existing user
    And AI question responses for instructions mapping is:
      | instruction            | expected_question_stem                          |
      | general question       | What is scuba diving?                           |
      | Relate to Singapore    | What is a good scuba diving place in Singapore? |

  Scenario: I should be able to affect the question using note instruction
    Given I have a note with the title "Scuba Diving"
    When I change the instruction of note "Scuba Diving" to "Relate to Singapore"
    Then the question stem generated from the note "Scuba Diving" should be "What is a good scuba diving place in Singapore?"
