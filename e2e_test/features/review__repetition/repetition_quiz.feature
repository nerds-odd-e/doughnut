Feature: Repetition Quiz
  As a learner, I want to use quizzes in my repetition to help and gamify my learning.

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topic    | details                          | skipReview | testingParent |
      | English  |                                | true       |               |
      | sedition | Sedition means incite violence | false      | English       |
      | sedation | Put to sleep is sedation       | false      | English       |

  Scenario: Cloze deletion question
    Given I learned one note "sedition" on day 1
    When I am repeat-reviewing my old note on day 2
    Then I should be asked cloze deletion question "[...] means incite violence" with options "sedition, sedation"

  Scenario Outline: Answering cloze question
    Given I learned one note "sedition" on day 1
    When I am repeat-reviewing my old note on day 2
    When I choose answer "<answer>"
    Then I should see that my answer <result>
    And I should see the repetition is finished: "<should see finish>"

    Examples:
      | answer   | result                  | should see finish |
      | sedation | "sedation" is incorrect | no                |
      | sedition | is correct              | yes               |

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
