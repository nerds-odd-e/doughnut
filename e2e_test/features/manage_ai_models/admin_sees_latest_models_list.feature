@usingMockedOpenAiService
@ignore
Feature: admin see latest models list

  As an admin,
  I want to manage AI models on admin page
  So that admin can choose AI models from openAI manually.

  Background:
    Given I am logged in as an admin and click AdminDashboard and go to tab "Manage Model"

  Scenario Outline: change question generation model for GPT4
    When I choose <training engine> for <model name> use
    Then I should be using for <training engine> for <model name>
    Examples:
      | training engine      | model name           |
      |  GPT 4               |  question generation |
      |  GPT 3.5             |  evaluation          |
      |  GET 3.5             |  others              |

  Scenario: load tab manage model
    When I choose "GPT 3.5" for "others" use
    Then I can choose the model from GPT in "question generation" dropdown list







