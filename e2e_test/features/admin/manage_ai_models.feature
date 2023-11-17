@usingMockedOpenAiService
Feature: Manage AI models

  As an admin,
  I want to manage AI models on admin page
  So that admin can choose which AI model to use for each task.

  Background:
    Given I am logged in as an admin
    And OpenAI returns text completion "A message from the future." for gpt model "gpt-future"
    And OpenAI has models "gpt-future, gpt-3.5" available
    And I have a note with the topic "Taiwan"

  Scenario: Admin choose a model for content completion
    When I choose model "gpt-future" for "Others"
    Then I request to complete the details for the note "Taiwan"
    And the note details on the current page should be "A message from the future."



