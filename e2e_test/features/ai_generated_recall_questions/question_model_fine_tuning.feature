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

  Scenario: User should be able to undo the marking
    Given I ask to generate a question for note "Who Let the Dogs Out"
    When I mark the question "Who wrote 'Who Let the Dogs Out'?" as good
    And I unmark the question "Who wrote 'Who Let the Dogs Out'?" as good
    Then an admin should be able to download the training data with 0 record
