@ignore
@withCliConfig
@interactiveCLI
Feature: CLI recall
  As a learner, I want to check recall status and run recall sessions in the interactive CLI.

  Background:
    Given I am logged in as an existing user
    And I set the access token for "old_learner" in the interactive CLI

  Rule: English notebook with two notes (sedition vs sedation; markdown in note content)

    Background:
      And I have a notebook "English practice" with notes:
        | Title    | Content                        |
        | English  |                                |
        | sedition | Sedition means incite violence |
        | sedation | **Put** to sleep is _sedation_ |
      And the notes "English" are skip-recalled

    @disableOpenAiService
    Scenario: Recall status shows count when notes are due
      Given the note "sedition" was assimilated on day 1
      And It's day 2
      When I enter the slash command "/recall-status" in the interactive CLI
      Then I should see "1 note to recall today" in past CLI assistant messages

    @disableOpenAiService
    Scenario: Just-review recall accepts remembered and declines load more
      Given the note "sedation" was assimilated on day 1
      And It's day 2
      When I enter the slash command "/recall" in the interactive CLI
      Then I should see "sedation" in the Current guidance
      And I should see "Put to sleep is sedation" in the Current guidance
      And I should see "Put" styled in the Current guidance
      And I should see "Yes, I remember?" in the Current guidance
      When I enter "y" in the interactive CLI
      Then I should see "sedation" in answered questions
      And I should see "Put to sleep" in answered questions
      And I should see "Reviewed: sedation" in answered questions
      When I answer "n" in the interactive CLI to prompt "Load more from next 3 days?"
      Then I should see "Recalled 1 note" in past CLI assistant messages

    @disableOpenAiService
    Scenario: Completing due notes then recalling from a later window
      Given the note "sedition" was assimilated on day 1
      And the note "sedation" was assimilated on day 1
      And It's day 2
      When I enter the slash command "/recall" in the interactive CLI
      And I answer "y" in the interactive CLI to prompt "Yes, I remember?"
      And I answer "y" in the interactive CLI to prompt "Yes, I remember?"
      And I answer "n" in the interactive CLI to prompt "Load more from next 3 days?"
      Then I should see "Recalled 2 notes" in past CLI assistant messages
      When I enter the slash command "/recall" in the interactive CLI
      And I answer "y" in the interactive CLI to prompt "Load more from next 3 days?"
      And I answer "y" in the interactive CLI to prompt "Yes, I remember?"
      Then I should see "Reviewed: sedition" in answered questions

    @usingMockedOpenAiService
    Scenario: MCQ recall accepts the correct choice
      Given OpenAI generates this question:
        | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 |
        | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   |
      And OpenAI evaluates the question as legitimate
      And the note "sedition" was assimilated on day 1
      And It's day 2
      When I enter the slash command "/recall" in the interactive CLI
      Then I should see "What is the meaning of sedition?" in the Current guidance
      And I should see "to incite violence" in the Current guidance
      When I enter "1" in the interactive CLI
      Then I should see "Correct!" in answered questions
      And I should see "sedition" in answered questions
      And I should see "What is the meaning of sedition?" in answered questions
      And I should see "to incite violence" in answered questions

    @usingMockedOpenAiService
    Scenario: MCQ recall rejects the next choice
      Given OpenAI generates this question:
        | Question Stem                    | Correct Choice     | Incorrect Choice 1 | Incorrect Choice 2 |
        | What is the meaning of sedition? | to incite violence | to sleep           | Open Water Diver   |
      And OpenAI evaluates the question as legitimate
      And the note "sedition" was assimilated on day 1
      And It's day 2
      When I enter the slash command "/recall" in the interactive CLI
      And I choose the next MCQ choice in the interactive CLI
      Then I should see "Incorrect" in answered questions
      And I should see "to sleep" in answered questions

  Rule: Spelling recall when the note has remember spelling enabled

    Background:
      And I have a notebook "English practice" with notes:
        | Title    | Content                        | Remember Spelling |
        | English  |                                |                   |
        | sedition | Sedition means incite violence | true              |
      And the notes "English" are skip-recalled

    @disableOpenAiService
    Scenario: Spelling recall accepts a correct answer then just review
      Given the note "sedition" was assimilated on day 1
      And It's day 2
      When I enter the slash command "/recall" in the interactive CLI
      Then I should see "means incite" in the Current guidance
      When I enter "sedition" in the interactive CLI
      Then I should see "Correct!" in answered questions
      And I should see "Your answer: sedition" in answered questions
      And I should see "sedition" in answered questions
      And I should see "Sedition means incite violence" in answered questions
      When I answer "y" in the interactive CLI to prompt "Yes, I remember?"
      Then I should see "sedition" in answered questions
      And I should see "Sedition means incite violence" in answered questions
      And I should see "Reviewed: sedition" in answered questions
      When I answer "n" in the interactive CLI to prompt "Load more from next 3 days?"
      Then I should see "Recalled 2 notes" in past CLI assistant messages
