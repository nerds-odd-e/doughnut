@withCliConfig
@disableOpenAiService
Feature: CLI recall status

  Background:
    Given I am logged in as an existing user
    And I have a valid Doughnut Access Token with label "Recall CLI Token"
    When I run the doughnut CLI add-access-token with the saved token
    Then I should see "Token added"

  Scenario: Recall status shows count when notes are due
    Given I have a notebook with the head note "English" which skips memory tracking
    And there are some notes:
      | Title    | Details                        | Parent Title |
      | sedition | Sedition means incite violence | English      |
      | sedation | Put to sleep is sedation       | English      |
    And It's day 1
    And I assimilate the note "sedition"
    And It's day 2
    When I run the doughnut command in interactive mode with input "/recall-status"
    Then I should see "1 note to recall today"

  Scenario: Recall status shows zero when no notes are due
    When I run the doughnut command in interactive mode with input "/recall-status"
    Then I should see "0 notes to recall today"
