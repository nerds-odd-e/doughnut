Feature: Assessment History
  As a trainee I want to see my assessment history,
  so that I can track my progress
  and review/share my results with others

  @ignore
  Scenario: "Empty history when no assessment has been done"
    Given I am logged in as an existing user
    When I go to the assessment history page
    Then I should see an empty assessment list

  @ignore
  Scenario: "View assessment history"
    Given I am logged in as an existing user
    When I have done the following assessments:
      | notebookTopic | score | totalQuestions |
      | Countries     | 2     | 5              |
      | Countries     | 5     | 5              |
      | Countries     | 0     | 2              |
    Then I should see the following assessments:
      | notebookTopic | score | totalQuestions |
      | Countries     | 2     | 5              |
      | Countries     | 5     | 5              |
      | Countries     | 0     | 2              |
