Feature: View my assessment history
  As a learner, I want to be able to view my past assessment

  Background:
    Given I am logged in as "old_learner"
    And there is an assessment on notebook "Just say 'Yes'" with 2 questions
    And there is an assessment on notebook "Also say 'Yes'" with 2 questions

  Scenario: Have not taken any assessment
    Then I should see my assessment history with empty records

  Scenario Outline: Have attempted assessment of a notebook
    Given I am logged in as "another_old_learner"
    When I get score <Score> when do the assessment on "Just say 'Yes'"
    Then I should see "Just say 'Yes'" result as "<Result>" in my assessment history
    And  I <Can view certificate or not> of "Just say 'Yes'" for "Another Old Learner" from "Old Learner" in my assessment history

    Examples:
      | Score | Result | Can view certificate or not |
      | 2/2   | Pass   | can view certificate        |
      | 1/2   | Fail   | can not view certificate    |
      | 0/2   | Fail   | can not view certificate    |

  Scenario: Get certificate for another assessment
    When I get score 2/2 when do the assessment on "Also say 'Yes'"
    Then I can view certificate of "Also say 'Yes'" for "Old Learner" from "Old Learner" in my assessment history
