@usingMockedOpenAiService
@startWithEmptyDownloadsFolder
Feature: Generate Training examples for fine-tuning OpenAI Question Generation
  As an admin,
  I want to use the good examples suggested by users for OpenAI fine-tuning
  to improve question generation.

  Background:
    Given I am logged in as an existing user
    And I've got the following question for a note with topic "Who Let the Dogs Out":
      | Question Stem                     | Correct Choice | Incorrect Choice 1 |
      | Who wrote 'Who Let the Dogs Out'? | Anslem Douglas | Baha Men           |


  Scenario: Admin should be able to generate training data from suggested questions
    When I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as a good example
    Then an admin can download the question generation training data containing:
      | Question Stem                     | Choices                  |
      | Who wrote 'Who Let the Dogs Out'? | Anslem Douglas, Baha Men |

  Scenario Outline: Training data should contain only the good examples
    When I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as a <Feedback> example
    Then an admin can download the question generation training data containing <Expected Number of Examples> examples

    Examples:
      | Feedback | Expected Number of Examples |
      | good     | 1                           |
      | bad      | 0                           |
