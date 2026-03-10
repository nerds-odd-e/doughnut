@withCliConfig
Feature: CLI recall status and recall next

  Background:
    Given I am logged in as an existing user
    And I have the CLI configured with a valid access token

  @disableOpenAiService
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

  @disableOpenAiService
  Scenario: Recall status shows zero when no notes are due
    When I run the doughnut command in interactive mode with input "/recall-status"
    Then I should see "0 notes to recall today"

  @disableOpenAiService
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

  @disableOpenAiService
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

  @disableOpenAiService
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

  @usingMockedOpenAiService
  Scenario: Recall next MCQ - choose correct answer and see success
    Given I have a notebook with the head note "English" which skips memory tracking
    And there are some notes:
      | Title    | Details                        | Parent Title |
      | sedition | Sedition means incite violence | English      |
      | sedation | Put to sleep is sedation       | English      |
    And OpenAI generates this question:
      | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   |
    And It's day 1
    And I assimilate the note "sedition"
    And It's day 2
    When I run the doughnut command in interactive mode with input "/recall next" and "1"
    Then I should see "What is the meaning of sedition?"
    And I should see "to incite violence"
    And I should see "Correct!"
    And I should see "Recalled successfully"

  @usingMockedOpenAiService
  Scenario: Recall next MCQ - down arrow and Enter to select
    Given I have a notebook with the head note "English" which skips memory tracking
    And there are some notes:
      | Title    | Details                        | Parent Title |
      | sedition | Sedition means incite violence | English      |
      | sedation | Put to sleep is sedation       | English      |
    And OpenAI generates this question:
      | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   |
    And It's day 1
    And I assimilate the note "sedition"
    And It's day 2
    When I run the doughnut command in interactive mode with down-arrow selection for "/recall next"
    Then I should see "Incorrect"
    And I should see "Recalled successfully"

  @disableOpenAiService
  Scenario: Recall session - complete all due notes and see summary
    Given I have a notebook with the head note "English" which skips memory tracking
    And there are some notes:
      | Title    | Details                        | Parent Title |
      | sedition | Sedition means incite violence | English      |
      | sedation | Put to sleep is sedation       | English      |
    And It's day 1
    And I assimilate the note "sedition"
    And I assimilate the note "sedation"
    And It's day 2
    When I run the doughnut command in interactive mode with input "/recall" and "y" and "y" and "n"
    Then I should see "Recalled 2 notes"

  @disableOpenAiService
  Scenario: Recall load more from next 3 days when none due today
    Given I have a notebook with the head note "English" which skips memory tracking
    And there are some notes:
      | Title    | Details                        | Parent Title |
      | sedition | Sedition means incite violence | English      |
      | sedation | Put to sleep is sedation       | English      |
    And It's day 1
    And I assimilate the note "sedition"
    And I assimilate the note "sedation"
    And It's day 2
    When I run the doughnut command in interactive mode with input "/recall" and "y" and "y" and "n"
    Then I should see "Recalled 2 notes"
    When I run the doughnut command in interactive mode with input "/recall" and "y" and "y" and "y" and "y"
    Then I should see "Load more from next 3 days?"
    And I should see "sedition"
    And I should see "Recalled successfully"
    And I should see "Recalled"

  @disableOpenAiService
  Scenario: Recall next spelling - type correct spelling and see success
    Given I have a notebook with the head note "English" which skips memory tracking
    And there are some notes:
      | Title    | Details                        | Parent Title |
      | sedition | Sedition means incite violence | English      |
      | sedation | Put to sleep is sedation       | English      |
    And It's day 1
    And I assimilate the note "sedition" with the option of remembering spelling
    And It's day 2
    When I run the doughnut command in interactive mode with input "/recall next" and "y"
    Then I should see "Recalled successfully"
    When I run the doughnut command in interactive mode with input "/recall next" and "sedition"
    Then I should see "Spell:"
    And I should see "Correct!"
    And I should see "Recalled successfully"
