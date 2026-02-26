@usingMockedOpenAiService
@startWithEmptyDownloadsFolder
Feature: Improve OpenAI Question Generation using User Feedback

  As a learner,
  I want to give feedback to the AI generated questions,
  so that the AI can learn from my feedback and improve its question generation.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Who Let the Dogs Out"
    And there are questions in the notebook "Who Let the Dogs Out" for the note:
      | Note Title           | Question                          | Answer         | One Wrong Choice |
      | Who Let the Dogs Out | Who wrote 'Who Let the Dogs Out'? | Anslem Douglas | Baha Men         |


  Scenario: Admin can obtain training data from positively evaluated questions
    When I suggest the question "Who wrote 'Who Let the Dogs Out'?" of the note "Who Let the Dogs Out" as a good example
    Then an admin can retrieve the training data for question generation containing:
      | Question Stem                     | Choices                  |
      | Who wrote 'Who Let the Dogs Out'? | Anslem Douglas, Baha Men |

  Scenario Outline: Training data inclusion is based on user feedback
    When I suggest the question "Who wrote 'Who Let the Dogs Out'?" of the note "Who Let the Dogs Out" as a <Feedback> example
    Then an admin can retrieve the training data for question generation containing <Expected Number of Examples> examples

    Examples:
      | Feedback | Expected Number of Examples |
      | good     | 1                           |
      | bad      | 0                           |
