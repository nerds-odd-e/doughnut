@withCliConfig
@interactiveCLI
Feature: CLI recall status and recall session

  Background:
    Given I am logged in as an existing user
    And I have a valid Doughnut Access Token with label "for cli"
    When I add the saved access token in the interactive CLI using add-access-token
    And I have a notebook with the head note "English" which skips memory tracking

  Rule: Shared English notebook notes (sedation details use markdown)

    Background:
      Given there are some notes:
        | Title    | Details                        | Parent Title |
        | sedition | Sedition means incite violence | English      |
        | sedation | **Put** to sleep is _sedation_ | English      |

    @disableOpenAiService
    Scenario: Recall status shows count when notes are due
      Given the note "sedition" was assimilated on day 1
      And It's day 2
      When I enter the slash command "/recall-status" in the interactive CLI
      Then I should see "1 note to recall today" in past CLI assistant messages

    @disableOpenAiService
    Scenario: Recall Just Review
      Given the note "sedation" was assimilated on day 1
      And It's day 2
      When I enter the slash command "/recall" in the interactive CLI
      Then I should see "sedation" in the Current guidance
      And I should see "Put to sleep is sedation" in the Current guidance
      And I should see "Put" styled in the Current guidance
      And I should see "Yes, I remember?" in the Current guidance
      When I enter "y" in the interactive CLI
      Then I should see "Recalled successfully" in past CLI assistant messages

    @ignore
    @disableOpenAiService
    Scenario: Temp1 - multiple notes in session
      Given the note "sedition" was assimilated on day 1
      And the note "sedation" was assimilated on day 1
      And It's day 2
      When I enter the slash command "/recall" in the interactive CLI
      When I answer "y" in the interactive CLI to prompt "Yes, I remember?"
      And I answer "y" in the interactive CLI to prompt "Yes, I remember?"

    @ignore
    @disableOpenAiService
    Scenario: Temp2 - ending session with n
      Given the note "sedition" was assimilated on day 1
      And It's day 2
      When I enter the slash command "/recall" in the interactive CLI
      When I answer "y" in the interactive CLI to prompt "Yes, I remember?"
      And I answer "n" in the interactive CLI to prompt "Load more from next 3 days?"
      Then I should see "Recalled 1 note" in past CLI assistant messages

    @ignore
    @disableOpenAiService
    Scenario: Recall session - complete all due notes, see summary, then load more from future days
      Given the note "sedition" was assimilated on day 1
      And the note "sedation" was assimilated on day 1
      And It's day 2
      When I enter the slash command "/recall" in the interactive CLI
      Then I should see "sedition" in the Current guidance
      When I answer "y" in the interactive CLI to prompt "Yes, I remember?"
      And I answer "y" in the interactive CLI to prompt "Yes, I remember?"
      And I answer "n" in the interactive CLI to prompt "Load more from next 3 days?"
      Then I should see "Recalled 2 notes" in past CLI assistant messages
      When I enter the slash command "/recall" in the interactive CLI
      And I answer "y" in the interactive CLI to prompt "Load more from next 3 days?"
      When I answer "y" in the interactive CLI to prompt "Yes, I remember?"
      Then I should see "Recalled successfully" in past CLI assistant messages

    @ignore
    @usingMockedOpenAiService
    Scenario: Recall MCQ - choose correct answer and see success
      And OpenAI generates this question:
        | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 |
        | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   |
      And the note "sedition" was assimilated on day 1
      And It's day 2
      When I enter the slash command "/recall" in the interactive CLI
      Then I should see "What is the meaning of sedition?" in the Current guidance
      And I should see "to incite violence" in the Current guidance
      When I enter "1" in the interactive CLI
      Then I should see "Correct!" in past CLI assistant messages
      And I should see "Recalled successfully" in past CLI assistant messages

    @ignore
    @usingMockedOpenAiService
    Scenario: Recall MCQ - ESC cancels with y/n confirmation
      And OpenAI generates this question:
        | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 |
        | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   |
      And the note "sedition" was assimilated on day 1
      And It's day 2
      When I enter the slash command "/recall" in the interactive CLI
      Then I should see "What is the meaning of sedition?" in the Current guidance
      When I enter the slash command "/stop" in the interactive CLI
      Then the recall session was stopped
      When I enter the slash command "/recall-status" in the interactive CLI
      Then I should see "1 note to recall today" in past CLI assistant messages

    @ignore
    @usingMockedOpenAiService
    Scenario: Recall MCQ - down arrow and Enter to select
      And OpenAI generates this question:
        | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 |
        | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   |
      And the note "sedition" was assimilated on day 1
      And It's day 2
      When I input down-arrow selection for "/recall" in the interactive CLI
      Then I should see "Incorrect" in past CLI assistant messages
      And I should see "Recalled successfully" in past CLI assistant messages

    @ignore
    @usingMockedOpenAiService
    Scenario: Recall MCQ - contest and regenerate before answering
      And OpenAI generates this as second question:
        | Question Stem         | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 |
        | Regenerated question? | to incite violence | to sleep           | Open Water Diver   |
      And OpenAI evaluates the question as not legitimate
      And OpenAI generates this as first question:
        | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 |
        | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   |
      And the note "sedition" was assimilated on day 1
      And It's day 2
      When I enter the slash command "/recall" in the interactive CLI
      Then I should see "What is the meaning of sedition?" in the Current guidance
      When I enter the slash command "/contest" in the interactive CLI
      Then I should see "What is the meaning of sedition?" in the Current guidance
      When I enter "1" in the interactive CLI
      Then I should see "Correct!" in past CLI assistant messages
      And I should see "Recalled successfully" in past CLI assistant messages

  Rule: Spelling recall needs remember spelling on sedition

    Background:
      Given there are some notes:
        | Title    | Details                        | Parent Title | Remember Spelling |
        | sedition | Sedition means incite violence | English      | true              |
        | sedation | **Put** to sleep is _sedation_ | English      |                   |

    @ignore
    @disableOpenAiService
    Scenario: Recall spelling - type correct spelling and see success
      And the note "sedition" was assimilated on day 1
      And It's day 2
      When I enter the slash command "/recall" in the interactive CLI
      Then I should see "Yes, I remember?" in the Current guidance
      When I enter "y" in the interactive CLI
      Then I should see "Spell:" in the Current guidance
      When I enter "sedition" in the interactive CLI
      Then I should see "Correct!" in past CLI assistant messages
      And I should see "Recalled successfully" in past CLI assistant messages
