@usingMockedOpenAiService
Feature: Repetition Quiz
  As a learner, I want to use quizzes in my repetition to help and gamify my learning.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "English" which skips review
    And there are some notes:
      | Title            | Details                        | Parent Title|
      | sedition         | Sedition means incite violence | English     |
      | sedation         | Put to sleep is sedation       | English     |
    And the OpenAI service is unavailable due to invalid system token

  Scenario Outline: Spelling quiz
    Given I am assimilating new note on day 1
    And I have selected the choice "Remember Spelling"
    When I am recalling my note on day 2
    Then I should be asked spelling question "means incite violence" from notebook "English"
    When I type my answer "<answer>"
    Then I should see that my answer <result>

    Examples:
      | answer   | result              |
      | asdf     | "asdf" is incorrect |
      | Sedition | is correct          |
