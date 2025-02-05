Feature: Repetition Quiz
  As a learner, I want to use quizzes in my repetition to help and gamify my learning.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "English" which skips review
    And there are some notes:
      | Title            | Details                        | Skip Memory Tracking| Parent Title|
      | sedition         | Sedition means incite violence | false      | English     |
      | sedation         | Put to sleep is sedation       | false      | English     |
      | medical          |                                | true       | English     |

  @usingMockedOpenAiService
  Scenario Outline: AI generated question
    Given OpenAI assistant will create these thread ids in sequence: "thread-first-question"
    And OpenAI generates this question for assistant thread "thread-first-question":
      | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   |
    And I learned one note "sedition" on day 1
    When I am recalling my note on day 2
    Then I should be asked "What is the meaning of sedition?"
    When I choose answer "<answer>"
    Then I should see that my answer <result>
    Examples:
      | answer             | result                  |
      | to sleep           | "to sleep" is incorrect |
      | to incite violence | is correct              |
