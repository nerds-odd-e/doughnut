@usingMockedOpenAiService
Feature: Question generation AI model evaluation
  As an admin, I want to use AI and specify a custom model to generate review questions based on my note and its context.
  So that I can review the quality of the custom model by viewing the generated questions.

  Scenario: I should be able to use a custom model to generate question
    Given I've logged in as "admin"
    And I have a note with the topic "Scuba Diving"
    And OpenAI by default returns this question from now:
      | question                                            | correct_choice | incorrect_choice_1 | incorrect_choice_2 |
      | What is the most common scuba diving certification? | Rescue Diver   | Divemaster         | Open Water Diver   |
    When I ask to generate a question for note "Scuba Diving" using custom model "gpt-4" and temperature 0.9
    Then I should be asked "What is the most common scuba diving certification?"
