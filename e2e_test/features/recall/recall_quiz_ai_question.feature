@usingMockedOpenAiService
Feature: AI recall quiz
  As a learner, I want AI-generated quizzes in recall to help and gamify recall.

  Background:
    Given I am logged in as an existing user
    And OpenAI evaluates the question as legitimate

  Scenario: AI generated question - incorrect answer
    Given I have a notebook "English practice" with notes:
      | Title    | Content                        |
      | sedition | Sedition means incite violence |
    And OpenAI generates this question:
      | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 | Incorrect Choice 3 |
      | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   | to stay silent     |
    And the note "sedition" was assimilated on day 1
    When I visit recall for a due recall prompt on day 2
    Then I should be asked "What is the meaning of sedition?"
    When I choose answer "to sleep"
    Then I should see that my MCQ answer "to sleep" is incorrect
