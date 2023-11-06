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
    And OpenAI response "<upload_result>" when uploading fine tuning data
    And OpenAi response "<training_result>" when trigger fine tuning data
    When I trigger fine tuning
    Then I should see the message <message>
    Examples:
      | positive_count | negative_count | message                                     | upload_result | training_result |
      | 9              | 9              | "Positive feedback cannot be less than 10." | success       | success         |
      | 10             | 10             | "Upload failed."                                        | failed        | success        |
      | 10             | 10             | "Training failed."                                      | success       | failed         |
      | 10             | 10             | "Training is in progress."                  | success       | success         |
