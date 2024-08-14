@ignore
Feature: View my assessment history
  As a learner, I want to be able to view my past assessment

  Background:
    Given I am logged in as an existing user
    
  Scenario: Have not taken any assessment
    When I try to view my assessment history
    Then I should see my assessment history with empty records

  Scenario: Have take one assessment of a notebook
    When I try to view my assessment history
    Then I should see one record of the assessment

  Scenario Outline: Have attempted assessment of a notebook
    When I view my assessment history
    Then I should see <Notebook> result as <Result>

    Examples:
      | Notebook    | Attempt At           | Result |
      | Notebook A  | 01-Apr-2024 12:00PM  | Pass   |
      | Notebook B  | 02-Apr-2024 10:00PM  | Fail   |



