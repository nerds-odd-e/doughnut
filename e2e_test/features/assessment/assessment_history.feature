Feature: View my assessment history
  As a learner, I want to be able to view my past assessments

  Background:
    Given I am logged in as "old_learner"
    And there is an assessment on notebook "Just say 'Yes'" with 2 questions
    And there is an assessment on notebook "Also say 'Yes'" with 2 questions

  Scenario: Have not taken any assessment
    Then I should see my assessment history with empty records

  Scenario Outline: Have attempted assessment of a notebook
    When I get score <Score> when do the assessment on "Just say 'Yes'"
    Then I should see "Just say 'Yes'" result as "<Result>" in my assessment history
    And  I <Can view certificate or not> of "Just say 'Yes'" in my assessment history

    Examples:
      | Notebook title | Score | Result | Can view certificate or not |
      | Just say 'Yes' | 2/2   | Pass   | can view certificate        |
      | Also say 'Yes' | 2/2   | Pass   | can view certificate        |
      | Just say 'Yes' | 1/2   | Fail   | can not view certificate    |
