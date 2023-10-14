@usingMockedOpenAiService
@startWithEmptyDownloadsFolder
Feature: Improve OpenAI Question Generation using User Feedback

  As an admin,
  I want to collect and utilize good examples suggested by users for OpenAI fine-tuning
  to enhance the quality of question generation.

  Background:
    Given I am logged in as an existing user
    And I've got the following question for a note with topic "Who Let the Dogs Out":
      | Question Stem                     | Correct Choice | Incorrect Choice 1 |
      | Who wrote 'Who Let the Dogs Out'? | Anslem Douglas | Baha Men           |


  Scenario: Admin can obtain training data from positively reviewed questions
    When I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as a good example
    Then an admin can download the question generation training data containing:
      | Question Stem                     | Choices                  |
      | Who wrote 'Who Let the Dogs Out'? | Anslem Douglas, Baha Men |

  Scenario Outline: Training data inclusion is based on user feedback
    When I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as a <Feedback> example
    Then an admin can download the question generation training data containing <Expected Number of Examples> examples

    Examples:
      | Feedback | Expected Number of Examples |
      | good     | 1                           |
      | bad      | 0                           |
