@usingMockedOpenAiService
@startWithEmptyDownloadsFolder
Feature: admin see latest models list

  As an admin,
  I want to manage AI models on admin page
  So that admin can choose AI models from openAI manually.

  Background:
    Given I am logged in as an admin and click AdminDashboard and go to tab "Manage Model"

  Scenario: change question generation model for GPT4
    When I choose "GPT 4" for "question generation" use
    Then I should be using for "GPT 4" for "question generation"

  Scenario: change evaluation model for GPT3.5
    When I choose "GPT 3.5" for "evaluation" use
    Then I should be using for "GPT 3.5" for "evaluation"

  Scenario: change others model for GPT4
    When I choose "GPT 3.5" for "others" use
    Then I should be using for "GPT 3.5" for "others"

  Scenario: load tab manage model
    When I choose "GPT 3.5" for "others" use
    Then I can choose the model from GPT in "question generation" dropdown list







