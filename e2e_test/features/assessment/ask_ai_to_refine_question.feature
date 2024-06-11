Feature: Ask AI to refine the question
  As a trainer, I want to create the question by asking the AI to refine the question,
  so that I can use the questions for assessment.

  Background:
    Given I am logged in as an existing user
    And I have a note with the topic "Countries"
    And I want to create a question for the note "Countries"

  @ignore
  Scenario: Cannot refine the question without any data
    When I don't fill any data
    Then The "Refine" button should be disabled

  @ignore
  Scenario: Can refine the question with the all full fill data
    Given I input data to all items of question:
    | Stem | Choice 0 | Choice 1 | Correct Choice Index |
    | Vietnam food | Pho | Pizza | 0 |
    When I click on the "Refine" button
    Then The refine result popup should be shown
    And The Correct Choice Index should be same as the request
    When I click on the "Accept" button
    Then The popup should be closed
    And The refined question should be filled in the form



