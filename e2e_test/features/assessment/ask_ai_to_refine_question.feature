@usingMockedOpenAiService
Feature: Ask AI to refine the question
  As a trainer, I want to create questions by asking the AI to adjust them properly,
  so that I can use them for our assessments.

  Background:
    Given I am logged in as an existing user
    And OpenAI now generates refine question:
      | Stem         | Choice 0 | Choice 1 | Correct Choice Index |
      | Vietnam food | Com Tam  | Hambuger | 0                    |
    And I have a note with the topic "Countries"
    And I want to create a question for the note "Countries"

  Scenario: Cannot refine the question without any information
    Then The "Refine" button should be disabled

  @ignore
  Scenario Outline: Can refine the question with filled data
  Given I fill "<Stem>" to the Stem of the question
  And I fill "<Choice 0>" to the Choice 0 of the question
  And I fill "<Choice 1>" to the Choice 1 of the question
  And I fill "<Correct Choice Index>" to the Correct Choice Index of the question
  When I refine the question
  Then The refined question's "Stem" should not empty
  And The refined question's "Choice 0" should not empty
  And The refined question's "Choice 1" should not empty
  And The refined question's Correct Choice Index should have the same "<Correct Choice Index>" as the original question if it isn't empty

    Examples:
    | Stem         | Choice 0  | Choice 1 | Correct Choice Index |
    | Vietnam food | Com Tam   | Hambuger | 0                    |
    | Vietnam food |           |          | 1                    |
    |              |  Pho      | Pizza    | 0                    |
    | Vietnam food |           |          |                      |
