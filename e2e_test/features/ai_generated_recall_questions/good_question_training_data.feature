@usingMockedOpenAiService
Feature: Generate Training Data from marked questions
  As a developer, I want to extract marked good questions
  So that I can provide in a format for OpenAI training data format for model trainer

  Background:
    Given I've logged in as "developer"
    And I have a note with the topic "Who Let the Dogs Out"
    And OpenAI by default returns this question from now:
      | question                          | correct_choice | incorrect_choice_1 |
      | Who wrote 'Who Let the Dogs Out'? | Anslem Douglas | Baha Men           |

  Scenario: No question has been marked by any user
    When I attempt to export
    Then I should return an empty JSONL file

  Scenario: I should be able to mark the question as good and undo the marking
    When I ask to generate a question for note "Who Let the Dogs Out"
    When I mark the question "Who wrote 'Who Let the Dogs Out'?" as good
    Then I should see the question "Who wrote 'Who Let the Dogs Out'?" is marked as good
    When I unmark the question "Who wrote 'Who Let the Dogs Out'?" as good
    Then I should see the question "Who wrote 'Who Let the Dogs Out'?" is not marked as good

  Scenario: 1 or more good questions
    When I ask to generate a question for note "Who Let the Dogs Out"
    When I mark the question "Who wrote 'Who Let the Dogs Out'?" as good
    Then I should see the question "Who wrote 'Who Let the Dogs Out'?" is marked as good
    When I attempt to export
    Then a file with training data is produced
