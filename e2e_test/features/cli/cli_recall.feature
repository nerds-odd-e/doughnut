@withCliConfig
Feature: CLI recall status and recall session

  Background:
    Given I am logged in as an existing user
    And I have a valid Doughnut Access Token with label "for cli"
    And I run the doughnut CLI add-access-token with the saved token

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
    Then I should see "1 note to recall today" in the history output

  @disableOpenAiService
  Scenario: Recall status shows zero when no notes are due
    When I run the doughnut command in interactive mode with input "/recall-status"
    Then I should see "0 notes to recall today" in the history output

  @disableOpenAiService
  Scenario Outline: Recall Just Review
    Given I have a notebook with the head note "English" which skips memory tracking
    And there are some notes:
      | Title    | Details                        | Parent Title |
      | <title>  | <details>                      | English      |
    And It's day 1
    And I assimilate the note "<title>"
    And It's day 2
    When I run the doughnut command in interactive mode with input "/recall" and "y"
    Then I should see "<title>" in the status
    And I should see "<content_check>" in the status
    And I should see "Yes, I remember?" in the status
    And I should see "Recalled successfully" in the history output

    Examples:
      | title    | details                              | content_check |
      | sedition | Sedition means incite violence       | sedition      |
      | sedation | **Put** to sleep is _sedation_       | Put           |

  @disableOpenAiService
  Scenario: Recall status shows zero after recalling the only note in session
    Given I have a notebook with the head note "English" which skips memory tracking
    And there are some notes:
      | Title    | Details                        | Parent Title |
      | sedition | Sedition means incite violence | English      |
    And It's day 1
    And I assimilate the note "sedition"
    And It's day 2
    When I run the doughnut command in interactive mode with input "/recall" and "y"
    Then I should see "Recalled successfully" in the history output
    When I run the doughnut command in interactive mode with input "/recall-status"
    Then I should see "0 notes to recall today" in the history output

  @usingMockedOpenAiService
  Scenario: Recall MCQ - choose correct answer and see success
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
    When I run the doughnut command in interactive mode with input "/recall" and "1"
    Then I should see "What is the meaning of sedition?" in the status
    And I should see "to incite violence" in the status
    And I should see "Correct!" in the history output
    And I should see "Recalled successfully" in the history output

  @usingMockedOpenAiService
  Scenario: Recall MCQ - ESC cancels with y/n confirmation
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
    When I run the doughnut command in interactive mode with recall MCQ and cancel with ESC
    Then the recall session was stopped
    When I run the doughnut command in interactive mode with input "/recall-status"
    Then I should see "1 note to recall today" in the history output

  @usingMockedOpenAiService
  Scenario: Recall MCQ - down arrow and Enter to select
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
    When I run the doughnut command in interactive mode with down-arrow selection for "/recall"
    Then I should see "Incorrect" in the history output
    And I should see "Recalled successfully" in the history output

  @disableOpenAiService
  Scenario: Recall substate - /stop exits recall mode
    Given I have a notebook with the head note "English" which skips memory tracking
    And there are some notes:
      | Title    | Details                        | Parent Title |
      | sedition | Sedition means incite violence | English      |
      | sedation | Put to sleep is sedation       | English      |
    And It's day 1
    And I assimilate the note "sedition"
    And I assimilate the note "sedation"
    And It's day 2
    When I run the doughnut command in interactive mode with input "/recall" and "/stop"
    Then I stopped the recall during review

  @disableOpenAiService
  Scenario: Recall session - complete all due notes, see summary, then load more from future days
    Given I have a notebook with the head note "English" which skips memory tracking
    And there are some notes:
      | Title    | Details                        | Parent Title |
      | sedition | Sedition means incite violence | English      |
      | sedation | Put to sleep is sedation       | English      |
    And It's day 1
    And I assimilate the note "sedition"
    And I assimilate the note "sedation"
    And It's day 2
    When I run a recall session and recall all due notes, declining load more
    Then I should see "Recalled 2 notes" in the history output
    When I run a recall session with load more from future days
    Then I should see "Load more from next 3 days?" in the status
    And I should see "sedition" in the status
    And I should see "Recalled successfully" in the history output
    And I should see "Recalled" in the history output

  @usingMockedOpenAiService
  Scenario: Recall MCQ - contest and regenerate before answering
    Given I have a notebook with the head note "English" which skips memory tracking
    And there are some notes:
      | Title    | Details                        | Parent Title |
      | sedition | Sedition means incite violence | English      |
      | sedation | Put to sleep is sedation       | English      |
    And OpenAI generates this as second question:
      | Question Stem          | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 |
      | Regenerated question?  | to incite violence | to sleep           | Open Water Diver   |
    And OpenAI evaluates the question as not legitimate
    And OpenAI generates this as first question:
      | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   |
    And It's day 1
    And I assimilate the note "sedition"
    And It's day 2
    When I run the doughnut command in interactive mode with input "/recall" and "/contest" and "1" and "exit"
    Then I should see "What is the meaning of sedition?" in the status
    And I should see "Correct!" in the history output
    And I should see "Recalled successfully" in the history output

  @disableOpenAiService
  Scenario: Recall spelling - type correct spelling and see success
    Given I have a notebook with the head note "English" which skips memory tracking
    And there are some notes:
      | Title    | Details                        | Parent Title |
      | sedition | Sedition means incite violence | English      |
      | sedation | Put to sleep is sedation       | English      |
    And It's day 1
    And I assimilate the note "sedition" with the option of remembering spelling
    And It's day 2
    When I run the doughnut command in interactive mode with input "/recall" and "y" and "sedition"
    Then I should see "Spell:" in the status
    And I should see "Correct!" in the history output
    And I should see "Recalled successfully" in the history output
