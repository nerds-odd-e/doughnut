@usingMockedOpenAiService
@startWithEmptyDownloadsFolder

Feature: add a comment to a note_question_suggestion
  As a learner, I want to add a comment about a question I do not like, so
  this can be used by the admin to train AI

  Background:
    Given I've logged in as an existing user
    And I have a note with the topic "Who Let the Dogs Out"
    And OpenAI by default returns this question from now:
      | question                          | correct_choice | incorrect_choice_1 |
      | Who wrote 'Who Let the Dogs Out'? | Anslem Douglas | Baha Men           |

  Scenario: Add a comment to an existing note question
    Given I ask to generate a question for note "Who Let the Dogs Out"
    When I add comment "this sucks" on this question "Who wrote 'Who Let the Dogs Out'?"
    Then the admin should see "this sucks" in the downloaded file

