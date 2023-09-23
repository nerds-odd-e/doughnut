@usingMockedOpenAiService
@startWithEmptyDownloadsFolder
Feature: Generate Training Data for fine-tuning OpenAI
  As an admin,
  I want the users to be able to suggest good note questions or improvements for bad ones,
  So that I can use these data for OpenAI fine-tuning to improve question generation.

  Background:
    Given I am logged in as an existing user
    And I have a note with the topic "Who Let the Dogs Out"

  Scenario: Admin should be able to generate training data from marked questions
    And OpenAI by default returns this question:
      | question                          | correct_choice | incorrect_choice_1 |
      | Who wrote 'Who Let the Dogs Out'? | Anslem Douglas | Baha Men          |
    When I ask to generate a question for the note "Who Let the Dogs Out"
    And I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as a good example
    Then an admin should be able to download the training data containing 1 record with the question "Who wrote 'Who Let the Dogs Out'?"

  Scenario: Add a comment to an existing note question
    And OpenAI by default returns this question:
      | question                          | correct_choice | incorrect_choice_1 |
      | Who wrote 'Who Let the Dogs Out'? | Anslem Douglas | Baha Men           |
    Given I ask to generate a question for the note "Who Let the Dogs Out"
    When I add comment "this is a comment on a question we don't like" on the question "Who wrote 'Who Let the Dogs Out'?"
    Then the admin should see "this is a comment on a question we don't like" in the downloaded file

  Scenario Outline: User gives a suggestion for the question
    And OpenAI by default returns this question:
      | question                          | correct_choice | incorrect_choice_1 |
      | Blah blah blah                    | Anslem Douglas | Baha Men           |
    When I ask to generate a question for the note "Who Let the Dogs Out"
    And I suggest an improved "<option>" with "<suggestion>"
    Then an admin should be able to download the training data with "<suggestion>" as an improved "<option>"
    Examples:
      | option         | suggestion                                 |
      | Question       | Who wrote 'Who Let the Cats Out'?          |
      #| choice         | John Douglas                               |
      #| correct_choice | Baha Women                                 |
