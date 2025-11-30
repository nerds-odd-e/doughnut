Feature: Repetition Quiz
  As a learner, I want to use quizzes in my repetition to help and gamify my learning.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "English" which skips review
    And there are some notes:
      | Title    | Details                        | Skip Memory Tracking | Parent Title |
      | sedition | Sedition means incite violence | false                | English      |
      | sedation | Put to sleep is sedation       | false                | English      |
      | medical  |                                | true                 | English      |

  @usingMockedOpenAiService
  Scenario: AI generated question - incorrect answer
    Given OpenAI generates this question:
      | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   |
    And I learned one note "sedition" on day 1
    When I am recalling my note on day 2
    Then I should be asked "What is the meaning of sedition?"
    When I choose answer "to sleep"
    Then I should see that my MCQ answer "to sleep" is incorrect

  @usingMockedOpenAiService
  Scenario: AI generated question - correct answer
    Given OpenAI generates this question:
      | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   |
    And I learned one note "sedition" on day 1
    When I am recalling my note on day 2
    Then I should be asked "What is the meaning of sedition?"
    When I choose answer "to incite violence"
    Then I should see that my answer is correct as the last question
