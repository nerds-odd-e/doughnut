@usingMockedOpenAiService
@startWithEmptyDownloadsFolder
Feature: Generate Training examples for fine-tuning OpenAI
  As an admin,
  I want the users to be able to suggest good note questions or improvements for bad ones,
  So that I can use these data for OpenAI fine-tuning to improve question generation.

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
    Then an admin can download the question generation training data containing <Number_of_example_download> examples

    Examples:
      | Feedback | Number_of_example_download |
      | good     | 1                          |
      | bad      | 0                          |

  Scenario: Admin should be able to download both positive and negative feedbacks for training evaluation model
    Given I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as a good example
    And I have a note with the topic "ChatGPT"
    And OpenAI by default returns this question:
      | Question Stem                      | Correct Choice | Incorrect Choice 1 |
      | In which year is ChatGPT launched? | 2002           | 2001               |
    And I ask to generate a question for the note "ChatGPT"
    And I suggest the displayed question "In which year is ChatGPT launched?" as a bad example
    Then an admin should be able to download the training data for evaluation containing 2 examples
