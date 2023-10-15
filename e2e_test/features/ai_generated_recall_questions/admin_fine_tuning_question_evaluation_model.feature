@usingMockedOpenAiService
@startWithEmptyDownloadsFolder
Feature: Refine Training Examples for OpenAI's Question Evaluation Model

  As an admin,
  I want to use both positive and negative feedback examples for OpenAI's fine-tuning
  to enhance the accuracy of question generation evaluation.

  Note: The question generation evaluation assesses the quality of both the generated questions and the AI models generating them.

  Background:
    Given I am logged in as an existing user

  Scenario: Admin can retrieve feedback-based training examples
    When I have the true false question "Fire is hot" rated as a good example
    And I have the true false question "What is hot?" rated as a bad example
    Then an admin should be able to download the training data for evaluation containing 2 examples
