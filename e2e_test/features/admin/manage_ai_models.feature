Feature: Admin manage AI models

  As an admin,
  I want to manage AI models on admin page
  So that admin can choose AI models from openAI manually.

  Background:
    Given I am logged in as an admin
    And I navigate to the "Manage Model" section in the admin dashboard

  @ignore
  Scenario: change question generation model for GPT4
    When I choose "gpt-4-0314" for "Question Generation" use
    Then I should be using "Question Generation" for "gpt-4-0314"

  Scenario: load tab manage model
    Then I can choose model "gpt-3.5-turbo" from GPT in "Question Generation" dropdown list







