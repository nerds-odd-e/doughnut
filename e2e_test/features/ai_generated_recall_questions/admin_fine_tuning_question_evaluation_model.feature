@usingMockedOpenAiService
@startWithEmptyDownloadsFolder
Feature: Refine Training Examples for OpenAI's Question Evaluation Model

  As an admin,
  I want to use both positive and negative feedback examples for OpenAI's fine-tuning
  to enhance the accuracy of question generation evaluation.

  Background:
    Given I am logged in as an admin
    And I navigate to the "Fine Tuning Data" section in the admin dashboard


  Scenario: Admin can retrieve feedback-based training examples
    When I have the true false question "Fire is hot" rated as a good example
    And I have the true false question "What is hot?" rated as a bad example
    Then an admin should be able to download the training data for evaluation containing:
      | Question Stem                          | Good Question? |
      | Fire is hot                            | Yes            |
      | What is hot?                           | No             |

  Scenario Outline: Trigger fine tuning with feedbacks for Question model and Evaluation model
    Given I have <positive_count> positive feedbacks and <negative_count> negative feedbacks
    And OpenAI responds with "<upload_result>" when uploading fine-tuning data
    And OpenAI responds with "<training_result>" when triggering fine-tuning
    When I attempt to trigger fine-tuning
    Then I should see the message <message>

    Examples:
      | positive_count | negative_count | upload_result | training_result | message                                     |
      | 9              | 9              | success       | success         | "Positive feedback cannot be less than 10." |
      | 10             | 10             | failed        | success         | "Upload failed."                            |
      | 10             | 10             | success       | failed          | "Training failed."                          |
      | 10             | 10             | success       | success         | "Training initiated."                       |

