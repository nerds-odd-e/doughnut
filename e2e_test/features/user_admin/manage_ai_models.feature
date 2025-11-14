@usingMockedOpenAiService
Feature: Manage AI models

  As an admin,
  I want to manage AI models on admin page
  So that admin can choose which AI model to use for each task.

  Background:
    Given I am logged in as an admin
    And OpenAI has models "gpt-future, gpt-3.5" available

  Scenario: Admin choose a default model
    When I choose model "gpt-future" for "Others"



