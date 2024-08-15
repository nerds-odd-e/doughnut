Feature: View my assessment history
  As a learner, I want to be able to view my past assessment

  Background:
    Given I am logged in as an existing user
    And there is an assessment on notebook "Just say 'Yes'" with 2 questions

  Scenario: Have not taken any assessment
    When I view my assessment history
    Then I should see my assessment history with empty records

  Scenario: Have take one assessment of a notebook
    When I get score 2/2 when do the assessment on "Just say 'Yes'"
    And I view my assessment history
    Then I should see one record of the assessment
  
  Scenario Outline: Have attempted assessment of a notebook
    When I get score <Score> when do the assessment on "<Notebook>"
    And I view my assessment history
    Then I should see "<Notebook>" result as "<Result>"

    Examples:
      | Notebook        | Score | Attempt At          | Result |
      | Just say 'Yes'  | 2/2   | 01-Apr-2024 12:00PM | Pass   |
      | Just say 'Yes'  | 1/2   | 02-Apr-2024 12:00PM | Fail   |



