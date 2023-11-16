@usingMockedOpenAiService
Feature: Refine OpenAI's AI Model using user feedback

  As an admin,
  I want to use both positive and negative feedback examples for OpenAI's fine-tuning
  to enhance the accuracy of question generation and question evaluation.

  Background:
    Given I am logged in as an admin
    And I navigate to the "Fine Tuning Data" section in the admin dashboard


  Scenario Outline: Trigger fine tuning with feedbacks for Question model and Evaluation model
    Given I have 10 positive feedbacks and 2 negative feedbacks
    And OpenAI responds with "<upload_result>" when uploading fine-tuning data
    And OpenAI responds with "<training_result>" when triggering fine-tuning
    When I attempt to trigger fine-tuning
    Then I should see the message "<message>"

    Examples:
      | upload_result | training_result | message             |
      | failed        | success         | Upload failed.      |
      | success       | failed          | Trigger Fine-Tuning Failed:    |
      | success       | success         | Training initiated. |

  Scenario Outline: There should be at least 10 positive feedback to trigger fine-tuning
    Given I have <positive_count> positive feedbacks and <negative_count> negative feedbacks
    When I attempt to trigger fine-tuning
    Then I should see the message "Positive feedback cannot be less than 10."

    Examples:
      | positive_count | negative_count |
      | 9              | 0              |
      | 9              | 10             |

