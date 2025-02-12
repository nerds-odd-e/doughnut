@mockBrowserTime
@usingMockedOpenAiService
Feature: Browse answers and notes while recalling
  As a learner, I want to browse answers and notes while recalling
  so that I can pause recalling to review my answers and notes
  and go back to recalling when I am ready.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "English" which skips review
    And there are some notes:
      | Title    | Details                        | Skip Memory Tracking | Parent Title |
      | sedition | Sedition means incite violence | false                | English      |
      | sedation | Put to sleep is sedation       | false                | English      |
      | medical  |                                | true                 | English      |
    And the OpenAI service is unavailable due to invalid system token
    And I am assimilating new note on day 1
    And I assimilate with the option of remembering spelling

  @ignore
  Scenario: View last result when the quiz answer was correct
    Given I learned one note "sedation" on day 1
    When I am recalling my note on day 2
    And I skip one question
    And I type my answer "sedition"
    Then I should see that my last answer is correct
    And I should see the memory tracker info of note "sedition"
      | type     | Repetition Count |
      | normal   |                0 |
      | spelling |                1 |

  Scenario: Browse notes while recalling and come back
    Given I am recalling my note on day 2
    And I skip one question
    And I type my answer "riot"
    And I should see that my answer "riot" is incorrect
    When I visit all my notebooks
    And I go to the recalls page
    Then I should see that my answer "riot" is incorrect

  Scenario: I can remove a note from further recalls
    Given I am recalling my note on day 2
    And I skip one question
    And I type my answer "sedition"
    When choose to remove the last memory tracker from recalls
    Then On day 100 I should have "0/1/1" note for assimilation and "1/2/2" for recall
