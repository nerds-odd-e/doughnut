@withCliConfig
Feature: CLI recall status and recall session

  Background:
    Given I am logged in as an existing user
    And I have a valid Doughnut Access Token with label "for cli"
    And I run the doughnut CLI add-access-token with the saved token

  @disableOpenAiService
  @interactiveCLI
  Scenario: Recall status shows count when notes are due
    Given I have a notebook with the head note "English" which skips memory tracking
    And there are some notes:
      | Title    | Details                        | Parent Title |
      | sedition | Sedition means incite violence | English      |
      | sedation | Put to sleep is sedation       | English      |
    And It's day 1
    And I assimilate the note "sedition"
    And It's day 2
    When I input "/recall-status" in the interactive CLI
    Then I should see "1 note to recall today" in the history output

  @disableOpenAiService
  @interactiveCLI
  Scenario: Recall Just Review
    Given I have a notebook with the head note "English" which skips memory tracking
    And there are some notes:
      | Title    | Details                        | Parent Title |
      | sedation | **Put** to sleep is _sedation_ | English      |
    And It's day 1
    And I assimilate the note "sedation"
    And It's day 2
    When I input "/recall" in the interactive CLI
    Then I should see "sedation" in the Current guidance
    And I should see "Put to sleep is sedation" in the Current guidance
    And I should see "Put" styled in the Current guidance
    And I should see "Yes, I remember?" in the Current guidance
    When I input "y" in the interactive CLI
    Then I should see "Recalled successfully" in the history output

  @disableOpenAiService
  @interactiveCLI
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
    When I input "/recall" in the interactive CLI
    Then I should see "sedition" in the Current guidance
    When I answer "y" in the interactive CLI to prompt "Yes, I remember?"
    And I answer "y" in the interactive CLI to prompt "Yes, I remember?"
    And I answer "n" in the interactive CLI to prompt "Load more from next 3 days?"
    Then I should see "Recalled 2 notes" in the history output
    When I input "/recall" in the interactive CLI
    And I answer "y" in the interactive CLI to prompt "Load more from next 3 days?"
    When I answer "y" in the interactive CLI to prompt "Yes, I remember?"
    Then I should see "Recalled successfully" in the history output

  @usingMockedOpenAiService
  @interactiveCLI
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
    When I input "/recall" in the interactive CLI
    Then I should see "What is the meaning of sedition?" in the Current guidance
    And I should see "to incite violence" in the Current guidance
    When I input "1" in the interactive CLI
    Then I should see "Correct!" in the history output
    And I should see "Recalled successfully" in the history output

  @usingMockedOpenAiService
  @interactiveCLI
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
    When I input "/recall" in the interactive CLI
    Then I should see "What is the meaning of sedition?" in the Current guidance
    When I input "/stop" in the interactive CLI
    Then the recall session was stopped
    When I input "/recall-status" in the interactive CLI
    Then I should see "1 note to recall today" in the history output

  @usingMockedOpenAiService
  @interactiveCLI
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
    When I input down-arrow selection for "/recall" in the interactive CLI
    Then I should see "Incorrect" in the history output
    And I should see "Recalled successfully" in the history output

  @disableOpenAiService
  @interactiveCLI
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
    When I input "/recall" in the interactive CLI
    Then I should see "sedition" in the Current guidance
    And I should see "Yes, I remember?" in the Current guidance
    When I input "/stop" in the interactive CLI
    Then I stopped the recall during review

  @usingMockedOpenAiService
  @interactiveCLI
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
    When I input "/recall" in the interactive CLI
    Then I should see "What is the meaning of sedition?" in the Current guidance
    When I input "/contest" in the interactive CLI
    Then I should see "What is the meaning of sedition?" in the Current guidance
    When I input "1" in the interactive CLI
    Then I should see "Correct!" in the history output
    And I should see "Recalled successfully" in the history output

  @disableOpenAiService
  @interactiveCLI
  Scenario: Recall spelling - type correct spelling and see success
    Given I have a notebook with the head note "English" which skips memory tracking
    And there are some notes:
      | Title    | Details                        | Parent Title |
      | sedition | Sedition means incite violence | English      |
      | sedation | Put to sleep is sedation       | English      |
    And It's day 1
    And I assimilate the note "sedition" with the option of remembering spelling
    And It's day 2
    When I input "/recall" in the interactive CLI
    Then I should see "Yes, I remember?" in the Current guidance
    When I input "y" in the interactive CLI
    Then I should see "Spell:" in the Current guidance
    When I input "sedition" in the interactive CLI
    Then I should see "Correct!" in the history output
    And I should see "Recalled successfully" in the history output
