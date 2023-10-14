@usingMockedOpenAiService
@startWithEmptyDownloadsFolder
Feature: Generate Training examples for fine-tuning OpenAI Question Evaluation Model
  As an admin,
  I want to use both good and bad examples in the training data for OpenAI fine-tuning
  to improve question generation evaluation.

  Question generation evaluation will be used to evaluate the quality of the generated question,
  or the AI model that generates the question.

  Background:
    Given I am logged in as an existing user
    And I've got the following question for a note with topic "Who Let the Dogs Out":
      | Question Stem                     | Correct Choice | Incorrect Choice 1 |
      | Who wrote 'Who Let the Dogs Out'? | Anslem Douglas | Baha Men           |


  Scenario: Admin should be able to download both positive and negative feedbacks for training evaluation model
    Given I suggest the displayed question "Who wrote 'Who Let the Dogs Out'?" as a good example
    And I've got the following question for a note with topic "ChatGPT":
      | Question Stem                      | Correct Choice | Incorrect Choice 1 |
      | In which year is ChatGPT launched? | 2002           | 2001               |
    And I ask to generate a question for the note "ChatGPT"
    And I suggest the displayed question "In which year is ChatGPT launched?" as a bad example
    Then an admin should be able to download the training data for evaluation containing 2 examples
