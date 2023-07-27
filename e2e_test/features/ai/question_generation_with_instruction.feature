@usingMockedOpenAiService
Feature: Question generation by AI

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user:
      | title        | testingParent |
      | Sports       |               |
      | Water sports | Sports        |
      | Scuba Diving | Water sports  |
      | Land sports  | Sports        |
      | Running      | Land sports   |
    And AI will generate question for instruction:
      | instruction            | question_stem                                   |
      | general question       | What is scuba diving?                           |
      | Relate to Singapore    | What is a good scuba diving place in Singapore? |

  Scenario: I should be able to affect the question using note instruction
    When I change the instruction of note "Scuba Diving" to "Relate to Singapore"
    Then the question stem generated from the note "Scuba Diving" should be "What is a good scuba diving place in Singapore?"
