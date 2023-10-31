@usingMockedOpenAiService
@startWithEmptyDownloadsFolder
Feature: admin see latest models list

  As an admin,
  I want to manage AI models on admin page
  So that admin can choose AI models from openAI manually.

  Background:
    Given I am logged in as an admin and click AdminDashboard and go to tab "Manage Model"

  @ignore
  Scenario: Admin on AdminDashboard models management tab
    Given admin on AdminDashboard and clicks models management tab
    When see 3 rows to be choose:
      | Questions  Generation | Questions Generation models management |
      | Evaluation            | Evaluation models  management          |
      | Others                | Others  models  management             |
    Then will see all all models versions and default 3.5 and 4

  @ignore
  Scenario: Admin selects models for each category and click submit button
    Given I select models for each management tab
    When I click submit button
    Then selected models will be used

  @ignore
  Scenario: Admin doesn't selects model for all of categories
    Given I select models for question generation and evalutation category but I don't select model for others category
    When I click submit button
    Then submit button should be disabled




