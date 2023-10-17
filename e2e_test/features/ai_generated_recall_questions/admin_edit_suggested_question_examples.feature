@usingMockedOpenAiService
@startWithEmptyDownloadsFolder
Feature: Generate Training examples for fine-tuning OpenAI
  As an admin,
  I want to edit the example questions suggested by the users,
  So that I can fix the questions or create good example by modifying bad ones,
  so that I can use these data for OpenAI fine-tuning to improve question generation and question generation vevaluation.

  Background:
    Given I am logged in as an existing user
    And I've got the following question for a note with topic "Who Let the Dogs Out":
      | Question Stem                     | Correct Choice | Incorrect Choice 1 |
      | Who wrote 'Who Let the Dogs Out'? | Anslem Douglas | Baha Men           |


  Scenario: Admin should be able to edit the first question and choice suggested
    Given I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as a good example
    When an admin edit the question and choices "Who wrote 'Who Let the Dogs Out'?" with a different question:
      | Question Stem                              | Choice A |
      | Did Baha Men write 'Who Let the Dogs Out'? | Yes      |
    Then an admin can retrieve the training data for question generation containing:
      | Question Stem                              | Choices  |
      | Did Baha Men write 'Who Let the Dogs Out'? | Yes      |

  Scenario: Admin should be able to duplicate negative feedback
    When I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as a bad example
    Then an admin can duplicate the question "Who wrote 'Who Let the Dogs Out'?"
    And an admin should be able to see 2 examples containing "Who wrote 'Who Let the Dogs Out'?"
    And an admin should be able to identify the duplicated record

  Scenario: Admin should not be able to duplicate positive feedback
    When I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as a good example
    Then an admin should not be able to duplicate this feedback to the question "Who wrote 'Who Let the Dogs Out'?"
