@usingMockedOpenAiService
Feature: Conversation with AI
  As a learner, I want to converse with AI about the question generated.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user:
      | title        | description                                    |
      | Scuba Diving | The most common certification is Rescue Diver. |
    And OpenAI by default returns this question from now:
      | question                                            | correct_choice | incorrect_choice_1 | incorrect_choice_2 |
      | What is the most common scuba diving certification? | Rescue Diver   | Divemaster         | Open Water Diver   |
    And I ask to generate a question for note "Scuba Diving"

  @ignore
  Scenario: I should be able to start a conversation and AI replies to me
    When the option "Rescue Diver" should be correct
    And I type in "blah blah" in the chat box
    Then the ai replies "blah blah blah"
