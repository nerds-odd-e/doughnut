@usingMockedOpenAiService
@startWithEmptyDownloadsFolder
Feature: Generate Training Data from marked questions
  As an admin, I want to extract marked good questions
  So that I can provide in a format for OpenAI training data format for model trainer

  Background:
    Given I've logged in as an existing user
    And I have a note with the topic "Who Let the Dogs Out"
    And OpenAI by default returns this question from now:
      | question                          | correct_choice | incorrect_choice_1 |
      | Who wrote 'Who Let the Dogs Out'? | Anslem Douglas | Baha Men           |
  
  Scenario: Admin should be able to generate training data from marked questions
    Given I ask to generate a question for note "Who Let the Dogs Out"
    When I mark the question "Who wrote 'Who Let the Dogs Out'?" as good
    Then an admin should be able to download the training data with 1 record
  
  Scenario: User haven't marked any question as good
    When I ask to generate a question for note "Who Let the Dogs Out"
    Then an admin should be able to download the training data with 0 record

  @ignore
  Scenario: User gives a suggestion for the stem part of the question
    When I ask to generate a question for note "Who Let the Dogs Out"
    And I suggest an improved question "Who wrote 'Who Let the Cats Out'?"
    Then an admin should be able to download the training data with improved question "Who wrote 'Who Let the Cats Out'?" 

  @ignore
  Scenario: User gives a suggestion for the choices part of the question
    When I ask to generate a question for note "Who Let the Dogs Out"
    And I suggest to replace one choice with "John Douglas"
    Then an admin should be able to download the training data with improved choice with "John Douglas" 

  @ignore
  Scenario: User gives a suggestion for the correct choice
    When I ask to generate a question for note "Who Let the Dogs Out"
    And I suggest the correct choice is "Baha Men"
    Then an admin should be able to download the training data with the improved correct choice "Baha Men"
