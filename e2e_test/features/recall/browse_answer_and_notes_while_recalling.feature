@mockBrowserTime
Feature: Browse answers and notes while recalling
  As a learner, I want to browse answers and notes while recalling
  so that I can pause recalling to review my answers and notes
  and go back to recalling when I am ready.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "English practice" with notes:
      | Title    | Content                        |
      | English  |                                |
      | sedition | Sedition means incite violence |
      | sedation | Put to sleep is sedation       |
      | medical  |                                |
    And the notes "English" are skipped from the assimilation sequence
    And It's day 1

  @disableOpenAiService
  @skipOptimizationDueToKnownNecessarySlowness
  Scenario: Browse notes while recalling and come back
    Given the note "sedition" was assimilated on day 1
    And I assimilate the note "sedition" with the option of remembering spelling
    When I visit recall for a due recall prompt on day 2
    And I type my answer "riot"
    Then I should see that my spelling answer "riot" is incorrect
    When I visit note "medical"
    Then I should be able to resume recalling
    When I resume recalling
    Then I should be back to the current question

  @disableOpenAiService
  Scenario: I can remove and revive a memory tracker from recalls
    Given the note "sedition" was assimilated on day 1
    And the note "sedition" was assimilated as spelling on day 1
    When I visit recall for a due recall prompt on day 2
    And I type my answer "sedition"
    And I choose to remove the last memory tracker from recalls
    Then On day 100 I should have "0/2/2" note for assimilation
    When I revive the memory tracker on this page
    Then the memory tracker should be available for recall again
