@usingMockedOpenAiService
@startWithEmptyDownloadsFolder
Feature: Generate Training Data for fine-tuning OpenAI
  As an admin,
  I want the users to be able to suggest good note questions or improvements for bad ones,
  So that I can use these data for OpenAI fine-tuning to improve question generation.

  Background:
    Given I am logged in as an existing user
    And I have a note with the topic "Who Let the Dogs Out"

  Scenario: Admin should be able to generate training data from suggested questions
    And OpenAI by default returns this question:
      | Question Stem                     | Correct Choice | Incorrect Choice 1 |
      | Who wrote 'Who Let the Dogs Out'? | Anslem Douglas | Baha Men           |

    When I ask to generate a question for the note "Who Let the Dogs Out"
    And I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as a good example
    Then an admin should be able to download the training data containing 1 record with the question "Who wrote 'Who Let the Dogs Out'?"

  Scenario: Add a comment when suggesting note question
    And OpenAI by default returns this question:
      | Question Stem                     | Correct Choice | Incorrect Choice 1 |
      | Who wrote 'Who Let the Dogs Out'? | Anslem Douglas | Baha Men           |
    Given I ask to generate a question for the note "Who Let the Dogs Out"
    And I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as an example but with comment "this is a comment on a question we don't like"
    Then the admin should see "this is a comment on a question we don't like" in the suggested questions

  Scenario Outline: User gives a suggestion for the question
    And OpenAI by default returns this question:
      | Question Stem                     | Correct Choice | Incorrect Choice 1 |
      | Blah blah blah                    | Anslem Douglas | Baha Men           |
    When I ask to generate a question for the note "Who Let the Dogs Out"
    And I suggest an improved "<option>" with "<suggestion>"
    Then an admin should be able to download the training data with "<suggestion>" as an improved "<option>"
    Examples:
      | option         | suggestion                                 |
      | Question       | Who wrote 'Who Let the Cats Out'?          |
      #| choice         | John Douglas                               |
      #| Correct Choice | Baha Women                                 |
