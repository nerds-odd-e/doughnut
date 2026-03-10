@withCliConfig
@disableOpenAiService
Feature: CLI recall status and recall next

  Background:
    Given I am logged in as an existing user
    And I have the CLI configured with a valid access token

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

  Scenario: Recall next Just Review - answer yes and verify success
    Given I have a notebook with the head note "English" which skips memory tracking
    And there are some notes:
      | Title    | Details                        | Parent Title |
      | sedition | Sedition means incite violence | English      |
      | sedation | Put to sleep is sedation       | English      |
    And It's day 1
    And I assimilate the note "sedition"
    And It's day 2
    When I run the doughnut command in interactive mode with input "/recall next" and "y"
    Then I should see "sedition"
    And I should see "Yes, I remember?"
    And I should see "Recalled successfully"

  Scenario: Recall next shows zero after recalling the only note
    Given I have a notebook with the head note "English" which skips memory tracking
    And there are some notes:
      | Title    | Details                        | Parent Title |
      | sedition | Sedition means incite violence | English      |
    And It's day 1
    And I assimilate the note "sedition"
    And It's day 2
    When I run the doughnut command in interactive mode with input "/recall next" and "y"
    Then I should see "Recalled successfully"
    When I run the doughnut command in interactive mode with input "/recall next"
    Then I should see "0 notes to recall today"

  Scenario: Recall next Just Review shows markdown note content
    Given I have a notebook with the head note "English" which skips memory tracking
    And there are some notes:
      | Title    | Details                         | Parent Title |
      | sedation | **Put** to sleep is _sedation_ | English      |
    And It's day 1
    And I assimilate the note "sedation"
    And It's day 2
    When I run the doughnut command in interactive mode with input "/recall next" and "y"
    Then I should see "sedation"
    And I should see "Put"
    And I should see "Yes, I remember?"
    And I should see "Recalled successfully"
