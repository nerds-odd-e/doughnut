@usingMockedOpenAiService
Feature: Manage AI models

  As an admin,
  I want to manage AI models on admin page
  So that admin can choose which AI model to use for each task.

  Background:
    Given I am logged in as an admin
    And OpenAI creates an assistant of ID "new_assistant" for name "note details completion" with model "gpt-future"
    And OpenAI has models "gpt-future, gpt-3.5" available

  Scenario: Admin choose a default model and use it for creating assistants
    When I choose model "gpt-future" for "Others"
    Then I recreate all the assitants and the new assistant ID should be "new_assistant" for "note details completion"



