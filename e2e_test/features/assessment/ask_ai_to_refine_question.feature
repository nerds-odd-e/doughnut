Feature: Ask AI to refine the question
  As a trainer, I want to create questions by asking the AI to adjust them properly,
  so that I can use them for our assessments.

  Background:
    Given I am logged in as an existing user
    And I have a note with the topic "Countries"
    And I want to create a question for the note "Countries"

  Scenario: Cannot refine the question without any information
    Then The "Refine" button should be disabled

  @ignore
  Scenario: Can refine the question with all data filled
    Given I input data into all items of question:
    | Stem | Choice 0 | Choice 1 | Correct Choice Index |
    | Vietnam food | Pho | Pizza | 0 |
    When I refine the question
    Then The result should be shown in the popup
    And The Correct Choice Index's result should be same as the request
    When I accept the result
    Then The popup should be closed
    And The result should be filled into the form

  @ignore
  Scenario: Can refine the question with filling data for "Stem", "number of choices" and "Correct Choice Index" only
    Given I input data into "Stem" and "Correct Choice Index" of question:
      | Stem | Choice 0 | Choice 1 | Correct Choice Index |
      | Vietnam food |  |  | 1 |
    When I refine the question
    Then The result should be shown in the popup
    And The Correct Choice Index's result should be same as the request
    When I accept the result
    Then The popup should be closed
    And The result should be filled into the form

  @ignore
  Scenario: Can refine the question with filling data for "Choices", "number of choices" and "Correct Choice Index" only
    Given I input data into "Choice 0", "Choice 1" and "Correct Choice Index" of question:
      | Stem | Choice 0 | Choice 1 | Correct Choice Index |
      |  | Pho | Pizza | 0 |
    When I refine the question
    Then The result should be shown in the popup
    And The Correct Choice Index's result should be same as the request
    When I accept the result
    Then The popup should be closed
    And The result should be filled into the form

  @ignore
  Scenario: Can refine the question with filling data for "Stem" and "number of choices" only
    Given I input data into "Stem" of the question:
      | Stem | Choice 0 | Choice 1 | Correct Choice Index |
      | Vietnam food |  |  |  |
    When I refine the question
    Then The result should be shown in the popup
    When I accept the result
    Then The popup should be closed
    And The result should be filled into the form
