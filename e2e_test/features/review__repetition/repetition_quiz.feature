Feature: Repetition Quiz
  As a learner, I want to use quizzes in my repetition to help and gamify my learning.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "English" which skips review
    And there are some notes:
      | Topic            | Details                        | Parent Topic|
      | sedition         | Sedition means incite violence | English     |
      | sedation         | Put to sleep is sedation       | English     |

  Scenario: Cloze deletion question
    Given I learned one note "sedition" on day 1
    When I am repeat-reviewing my old note on day 2
    Then I should be asked cloze deletion question "[...] means incite violence" with options "sedition, sedation"

  Scenario: Answering cloze question incorrectly
    Given I learned one note "sedition" on day 1
    When I am repeat-reviewing my old note on day 2
    When I choose answer "sedation"
    Then I should see that my answer "sedation" is incorrect
    And I should see the repetition is finished: "no"

  Scenario: Answering cloze question correctly
    Given I learned one note "sedition" on day 1
    When I am repeat-reviewing my old note on day 2
    When I choose answer "sedition"
    Then I should see that my answer is correct
    And I should see the repetition is finished: "yes"

  Scenario Outline: Spelling quiz
    Given I am learning new note on day 1
    And I have selected the choice "Remember Spelling"
    When I am repeat-reviewing my old note on day 2
    Then I should be asked spelling question "means incite violence" from notebook "English"
    When I type my answer "<answer>"
    Then I should see that my answer <result>

    Examples:
      | answer   | result              |
      | asdf     | "asdf" is incorrect |
      | Sedition | is correct          |
