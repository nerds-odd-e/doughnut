Feature: Repetition Quiz
  As a learner, I want to use quizzes in my repetition to help and gamify my learning.

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topic    | details                          | skipReview | testingParent |
      | English  |                                | true       |               |
      | sedition | Sedition means incite violence | false      | English       |
      | sedation | Put to sleep is sedation       | false      | English       |
      | medical  |                                | true       | English       |

  @usingMockedOpenAiService
  Scenario Outline: AI generated question
    Given I opt to do only AI generated questions
    And OpenAI now generates this question:
      | Question Stem                    | Correct Choice        | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is the meaning of sedition? |  to incite violence   | to sleep           | Open Water Diver   |
    And I learned one note "sedition" on day 1
    When I am repeat-reviewing my old note on day 2
    Then I should be asked "What is the meaning of sedition?"
    When I choose answer "<answer>"
    Then I should see that my answer <result>
    Examples:
      | answer             | result                    |
      | to sleep           | "to sleep" is incorrect   |
      | to incite violence | is correct                |
