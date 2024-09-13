@usingMockedOpenAiService
Feature: Repetition Quiz
  As a learner, I want to use quizzes in my repetition to help and gamify my learning.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "English" which skips review
    And there are some notes:
      | Topic            | Details                        | Skip Review| Parent Topic|
      | sedition         | Sedition means incite violence | false      | English     |
      | sedation         | Put to sleep is sedation       | false      | English     |
      | medical          |                                | true       | English     |
    And the OpenAI service is unavailable due to invalid system token
    And I am learning new note on day 1
    And I have selected the choice "Remember Spelling"

  Scenario: View last result when the quiz answer was correct
    And I learned one note "sedation" on day 1
    When I am repeat-reviewing my old note on day 2
    And I type my answer "sedition"
    Then I should see that my last answer is correct
    And I should see the review point info of note "sedition"
      | Repetition Count |
      | 1                |

  @mockBrowserTime
  Scenario: I can remove a note from further reviews
    And I am repeat-reviewing my old note on day 2
    And I type my answer "sedition"
    When choose to remove the last review point from reviews
    Then On day 100 I should have "1/1" note for initial review and "0/0" for repeat
