@usingMockedOpenAiService
Feature: Manage AI models

  As an admin,
  I want to manage AI models on admin page
  So that admin can choose which AI model to use for each task.

  Background:
    Given I am logged in as an admin
    And OpenAI returns text completion "A message from the future." for model "gpt-future"
    And OpenAI has models "gpt-future, gpt-3.5" available
    And I have a note with the topic "Taiwan"

  Scenario: Admin choose a model for content completion
    When I choose model "gpt-future" for "Others"
    Then I ask to complete the details for note "Taiwan"
    And I should see the note details on current page becomes "A message from the future."



