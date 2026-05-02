@disableOpenAiService
Feature: Recall Quiz
  As a learner, I want to use quizzes in my recall to help and gamify my learning.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "English practice" with notes:
      | Title    | Details                        | Skip Memory Tracking |
      | English  |                                | true                 |
      | sedition | Sedition means incite violence |                      |
      | sedation | Put to sleep is sedation       |                      |

  Scenario: Spelling quiz - incorrect answer
    Given It's day 1
    And I assimilate the note "sedition" with the option of remembering spelling
    When I am recalling my note on day 2
    Then I should be asked spelling question "means incite violence" from notebook "English practice"
    When I type my answer "asdf"
    Then I should see that my spelling answer "asdf" is incorrect

  Scenario: Spelling quiz - correct answer
    Given It's day 1
    And I assimilate the note "sedition" with the option of remembering spelling
    When I am recalling my note on day 2
    Then I should be asked spelling question "means incite violence" from notebook "English practice"
    When I type my answer "Sedition"
    Then I should see that my answer is correct
