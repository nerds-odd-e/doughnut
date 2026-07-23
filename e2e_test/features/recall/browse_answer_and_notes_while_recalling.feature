@mockBrowserTime
@disableOpenAiService
Feature: Browse answers and notes while recalling
  As a learner, I want to browse answers and notes while recalling
  so that I can pause recalling to review my answers and notes
  and go back to recalling when I am ready.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "English practice" with notes:
      | Title    | Content                        | Skip Memory Tracking | Remember Spelling |
      | English  |                                | true                 |                   |
      | sedition | Sedition means incite violence |                      | true              |
      | sedation | Put to sleep is sedation       |                      |                   |
      | medical  |                                |                      |                   |
    And It's day 1

  Scenario: View last answered question when the quiz answer was correct
    Given the note "sedition" was assimilated on day 1
    When I visit recall for a due quiz question on day 2
    And I type my answer "sedition"
    Then I should see that my last spelling answer was correct with recall count 1

  @skipOptimizationDueToKnownNecessarySlowness
  Scenario: Browse notes while recalling and come back
    Given I assimilate the note "sedition" with the option of remembering spelling
    When I visit recall for a due quiz question on day 2
    And I type my answer "riot"
    And I should see that my spelling answer "riot" is incorrect
    When I visit note "medical"
    Then I should see the resume recall menu item
    And I click resume recall from the menu
    Then I should be back to the current question

  Scenario: I can remove a note from further recalls
    Given the note "sedition" was assimilated on day 1
    When I visit recall for a due quiz question on day 2
    And I type my answer "sedition"
    When choose to remove the last memory tracker from recalls
    Then On day 100 I should have "0/2/2" note for assimilation

  Scenario: I can revive a memory tracker removed from recalls
    Given the note "sedition" was assimilated on day 1
    When I visit recall for a due quiz question on day 2
    And I type my answer "sedition"
    When choose to remove the last memory tracker from recalls
    And I revive the memory tracker on this page
