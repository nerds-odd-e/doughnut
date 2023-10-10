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

  Scenario: Admin should be able to generate training data from good suggested questions
    When I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as a good example
    Then an admin should be able to download the training data containing 1 example containing "Who wrote 'Who Let the Dogs Out'?"
    Then an admin should be able to download the training data containing 1 example containing "Baha Men"

  @ignore
  Scenario: Admin should not be able to generate training data from bad suggested questions
    When I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as a bad example
    Then an admin should be able to download the training data containing 0 examples

  @ignore
  Scenario: User should be able to mark the suggested question as a good example
    When I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as a good example
    Then then I should see a message saying the feedback was sent successfully
    And an admin should be able to download the training data containing 1 example containing "Who wrote 'Who Let the Dogs Out'?"

  @ignore
  Scenario: User should be able to mark the suggested question as a bad example
    When I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as a bad example
    Then then I should see a message saying the feedback was sent successfully
    And an admin should be able to download the training data containing 0 examples

  @ignore
  Scenario: User should be able to mark the suggested question as a bad example
    When I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as a good example with comment "This is awesome!"
    Then then I should see a message saying the feedback was sent successfully
    Then the admin should see "This is awesome!" in the suggested questions

  @ignore
  Scenario: User should be able to mark the suggested question as a bad example
    When I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as a bad example with comment "This is terrible!"
    Then then I should see a message saying the feedback was sent successfully
    Then the admin should see "This is terrible!" in the suggested questions

  Scenario: Admin edit the first question and choice suggested
    Given I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as a good example
    When an admin edit the question and choices "Who wrote 'Who Let the Dogs Out'?" with a different question:
      | Question Stem                              |Choice A|
      | Did Baha Men write 'Who Let the Dogs Out'? |Yes     |
    Then an admin should be able to download the training data containing 1 example containing "Did Baha Men write 'Who Let the Dogs Out'?"
    And an admin should be able to download the training data containing 1 example containing "Yes"
