@usingMockedOpenAiService
Feature: Question generation by AI
  As a learner, I want to use AI to generate review questions based on my note and its context.
  So that I can remember my note better and potentially get new inspiration.

  Background:
    Given I've logged in as an existing user
    And the AI question stem is "What is scuba diving?" when no instruction
    And the AI question stem is " What is a good scuba diving place in Singapore?" when the instruct is "Relate to Singapore"

  @ignore
  Scenario Outline: I should be able to affect the question using note instruction
    When there are some notes for the current user
      | title        | instruction   | 
      | Scuba Diving | <instruction> |
    Then Question stem generated from the note "Scuba Diving" should be "<expected question stem>"
    And it does <this action>

    Examples:
      | instruction            | expected question stem                          |
      |                        | What is scuba diving?                           |
      | Relate to Singapore    | What is a good scuba diving place in Singapore? |