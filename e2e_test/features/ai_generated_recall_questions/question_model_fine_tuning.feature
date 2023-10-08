@usingMockedOpenAiService
@startWithEmptyDownloadsFolder
Feature: Generate Training examples for fine-tuning OpenAI
  As an admin,
  I want the users to be able to suggest good note questions or improvements for bad ones,
  So that I can use these data for OpenAI fine-tuning to improve question generation.

  Background:
    Given I am logged in as an existing user
    And I have a note with the topic "Who Let the Dogs Out"
    And OpenAI by default returns this question:
      | Question Stem                     | Correct Choice | Incorrect Choice 1 |
      | Who wrote 'Who Let the Dogs Out'? | Anslem Douglas | Baha Men           |
    And I ask to generate a question for the note "Who Let the Dogs Out"

  Scenario: Admin should be able to generate training data from suggested questions
    When I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as a good example
    Then an admin should be able to download the training data containing 1 example containing "Who wrote 'Who Let the Dogs Out'?"
    Then an admin should be able to download the training data containing 1 example containing "Baha Men"

  Scenario: Add a comment when suggesting note question
    When I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as an example but with comment "I want deeper questions."
    Then the admin should see "I want deeper questions." in the suggested questions

  Scenario: Admin edit the question example suggested
    Given I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as a good example
    When an admin edit the question "Who wrote 'Who Let the Dogs Out'?" with a different question:
      | Question Stem                              |
      | Did Baha Men write 'Who Let the Dogs Out'? |
    Then an admin should be able to download the training data containing 1 example containing "Did Baha Men write 'Who Let the Dogs Out'?"
