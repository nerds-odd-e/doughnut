@usingMockedOpenAiService
Feature: Repetition Quiz
  As a learner, I want to use quizzes in my repetition to help and gamify my learning.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "English" which skips review
    And there are some notes:
      | Topic            | Details                        | Parent Topic|
      | sedition         | Sedition means incite violence | English     |
      | sedation         | Put to sleep is sedation       | English     |
    And the OpenAI service is unavailable due to invalid system token

  Scenario Outline: Spelling quiz
    Given I am learning new note on day 1
    And I have selected the choice "Remember Spelling"
    When I am repeat-reviewing my old note on day 2
    Then I should be asked spelling question "means incite violence" from notebook "English"
    When I type my answer "<answer>"
    Then I should see that my answer <result>
    And I should see the repetition is finished: "<should see finish>"

    Examples:
      | answer   | result              | should see finish |
      | asdf     | "asdf" is incorrect | no                |
      | Sedition | is correct          | yes               |
