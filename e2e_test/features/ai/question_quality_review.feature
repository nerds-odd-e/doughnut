@usingMockedOpenAiService
Feature: Question generation by AI using custom model
  As a developer, I want to use AI and specify a custom model to generate review questions based on my note and its context.
  So that I can review the quality of the custom model by viewing the generated questions.

  Scenario: I should be able to use a custom model to generate question
    Given I've logged in as "developer"
    And there are some notes for the current user:
      | topic        | details                                          |
      | Scuba Diving | The most common certification is Rescue Diver.   |
    And OpenAI by default returns this question from now:
      | question                                            | correct_choice | incorrect_choice_1 | incorrect_choice_2 |
      | What is the most common scuba diving certification? | Rescue Diver   | Divemaster         | Open Water Diver   |
    When I ask to generate a question for note "Scuba Diving" using custom model "gpt-4" and temperature 0.9
    Then I should be asked "What is the most common scuba diving certification?"

  Scenario: I should not be able to use a custom model to generate question as a learner
    Given I've logged in as an existing user
    And there are some notes for the current user:
      | topic        | details                                          |
      | Scuba Diving | The most common certification is Rescue Diver.   |
    And OpenAI by default returns this question from now:
      | question                                            | correct_choice | incorrect_choice_1 | incorrect_choice_2 |
      | What is the most common scuba diving certification? | Rescue Diver   | Divemaster         | Open Water Diver   |
    When I ask to generate a question for note "Scuba Diving"
    And I should not be able to see any input for custom model

  Scenario: I should see error when I enter an invalid custom model as a developer
    Given I've logged in as "developer"
    And there are some notes for the current user:
      | topic        | details                                          |
      | Scuba Diving | The most common certification is Rescue Diver.   |
    And An OpenAI response is unavailable
    When I ask to generate a question for note "Scuba Diving" using invalid custom model "my-custom-model"
    And I should see an error message "Invalid custom model input"
