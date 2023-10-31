@usingMockedOpenAiService
@startWithEmptyDownloadsFolder
@ignore
Feature: admin see latest models list

  As an admin,
  I want to manage AI models on admin page
  So that admin can choose AI models from openAI manually.

  Background:
    Given I am logged in as an admin,


  Scenario: Admin on AdminDashboard models management tab
    Given admin click AdminDashboard models management tab
    When see 3 rows to be choose:
      | Questions  Generation | Questions Generation models management |
      | Evaluation            | Evaluation models  management          |
      | Others                | Others  models  management             |
    Then will see all all models versions and default 3.5 and 4.



