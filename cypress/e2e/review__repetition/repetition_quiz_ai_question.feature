Feature: Repetition Quiz
  As a learner, I want to use quizzes in my repetition to help and gamify my learning.

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title    | description                    | skipReview | testingParent |
      | English  |                                | true       |               |
      | sedition | Sedition means incite violence | false      | English       |
      | sedation | Put to sleep is sedation       | false      | English       |
      | medical  |                                | true       | English       |

  @usingMockedOpenAiService
  Scenario Outline: AI generated question
    Given I opt to do only AI generated questions
    And OpenAI by default returns this question from now:
      | question                         | option_a  | option_b           |
      | What is the meaning of sedition? | to sleep  | to incite violence |
    And I learned one note "sedition" on day 1
    When I am repeat-reviewing my old note on day 2
    Then I should be asked "What is the meaning of sedition?"
    And the option "<option>" should be <expectedResult>
    Examples:
      | option       | expectedResult |
      | to incite violence | wrong        |
      | to sleep   | correct          |

    # When I choose answer "<answer>"
    # Then I should see that my answer <result>
    # Examples:
    #   | answer             | result              |
    #   | to sleep           | "to sleep" is wrong |
      # | to incite violence | is correct          |
