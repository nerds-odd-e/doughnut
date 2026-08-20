@usingMockedOpenAiService
Feature: Manage AI models

  As an admin,
  I want to choose which AI model is used for each task.

  Background:
    Given I have a session as "admin"
    And OpenAI has models "gpt-future, gpt-3.5" available

  Scenario: Admin chooses a default model
    When I choose model "gpt-future" for "Others"
    Then the model for "Others" should be "gpt-future"
