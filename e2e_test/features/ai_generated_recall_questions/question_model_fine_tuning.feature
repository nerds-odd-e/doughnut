@usingMockedOpenAiService
@startWithEmptyDownloadsFolder
Feature: Generate Training Data from marked questions
  As an admin, I want to extract marked good questions
  So that I can provide in a format for OpenAI training data format for model trainer

  Background:
    Given I've logged in as an existing user
    And I have a note with the topic "Who Let the Dogs Out"

  Scenario: Admin should be able to generate training data from marked questions
    And OpenAI by default returns this question from now:
      | question                          | correct_choice | incorrect_choice_1 |
      | Who wrote 'Who Let the Dogs Out'? | Anslem Douglas | Baha Men           |
    Given I ask to generate a question for note "Who Let the Dogs Out"
    When I mark the question "Who wrote 'Who Let the Dogs Out'?" as good
    Then an admin should be able to download the training data with 1 record containing "Who wrote 'Who Let the Dogs Out'?"

  Scenario Outline: User gives a suggestion for the question
    And OpenAI by default returns this question from now:
      | question                          | correct_choice | incorrect_choice_1 |
      | Blah blah blah                    | Anslem Douglas | Baha Men           |
    When I ask to generate a question for note "Who Let the Dogs Out"
    And I suggest an improved "<option>" with "<suggestion>"
    Then an admin should be able to download the training data with "<suggestion>" as an improved "<option>"
    Examples:
      | option         | suggestion                                 |
      | Question       | Who wrote 'Who Let the Cats Out'?          |
      #| choice         | John Douglas                               |
      #| correct_choice | Baha Women                                 |
